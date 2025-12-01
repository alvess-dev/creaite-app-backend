package com.creaite.wardrobe_api.controllers;

import com.creaite.wardrobe_api.domain.user.User;
import com.creaite.wardrobe_api.dto.GoogleTokenDTO;
import com.creaite.wardrobe_api.dto.LoginRequestDTO;
import com.creaite.wardrobe_api.dto.RegisterRequestDTO;
import com.creaite.wardrobe_api.dto.ResponseDTO;
import com.creaite.wardrobe_api.infra.security.TokenService;
import com.creaite.wardrobe_api.repositories.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Permite CORS para teste
public class AuthController {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Value("${GOOGLE_CLIENT_ID}")
    private String googleClientId;

    // Endpoint para verificar se o email já existe
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        try {
            log.info("=== Checking email: {} ===", email);

            // Validação básica do formato do email
            if (email == null || email.trim().isEmpty()) {
                log.warn("Empty email provided");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email is required"));
            }

            Optional<User> existingUser = this.repository.findByEmail(email.trim().toLowerCase());

            if (existingUser.isPresent()) {
                log.info("Email {} already exists", email);
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Email already registered"));
            }

            log.info("Email {} is available", email);
            return ResponseEntity.ok(Map.of("message", "Email available"));

        } catch (Exception e) {
            log.error("Error checking email: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error checking email: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO body) {
        try {
            log.info("=== Login attempt for email: {} ===", body.email());

            User user = this.repository.findByEmail(body.email())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.isOAuthUser()) {
                log.warn("OAuth user attempting email login");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "This account uses Google Sign-In. Please login with Google."));
            }

            if (passwordEncoder.matches(body.password(), user.getPassword())) {
                String token = this.tokenService.generateAccessToken(user);
                user.setLastLogin(LocalDateTime.now());
                this.repository.save(user);

                log.info("Login successful for user: {}", user.getEmail());
                return ResponseEntity.ok(new ResponseDTO(user.getName(), token));
            }

            log.warn("Invalid password for user: {}", body.email());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid email or password"));

        } catch (RuntimeException e) {
            log.error("Login error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequestDTO body) {
        try {
            log.info("=== Registration attempt for email: {} ===", body.email());

            Optional<User> existingUser = this.repository.findByEmail(body.email());

            if (existingUser.isPresent()) {
                log.warn("Email already registered: {}", body.email());
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email already registered"));
            }

            Optional<User> existingUsername = this.repository.findByUsername(body.username());
            if (existingUsername.isPresent()) {
                log.warn("Username already taken: {}", body.username());
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Username already taken"));
            }

            User newUser = new User();
            newUser.setPassword(passwordEncoder.encode(body.password()));
            newUser.setEmail(body.email().toLowerCase().trim());
            newUser.setUsername(body.username());
            newUser.setName(body.name());
            newUser.setBirthDate(body.birthDate());
            newUser.setLanguage(body.language() != null ? body.language() : "en");
            newUser.setStatus(User.UserStatus.ACTIVE);
            newUser.setIsVerified(false);
            this.repository.save(newUser);

            log.info("User registered successfully: {}", newUser.getEmail());

            String token = this.tokenService.generateAccessToken(newUser);
            return ResponseEntity.ok(new ResponseDTO(newUser.getName(), token));

        } catch (Exception e) {
            log.error("Registration error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Registration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleAuth(@RequestBody GoogleTokenDTO body) {
        try {
            log.info("=== Google authentication attempt ===");

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(body.idToken());

            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name");
                String picture = (String) payload.get("picture");

                log.info("Google auth successful for email: {}", email);

                User user = repository.findByEmail(email)
                        .orElseGet(() -> createGoogleUser(email, name, picture));

                user.setLastLogin(LocalDateTime.now());
                repository.save(user);

                String token = tokenService.generateAccessToken(user);
                return ResponseEntity.ok(new ResponseDTO(user.getName(), token));
            }

            log.warn("Invalid Google token");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid Google token"));

        } catch (Exception e) {
            log.error("Google auth error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Google authentication failed: " + e.getMessage()));
        }
    }

    private User createGoogleUser(String email, String name, String picture) {
        log.info("Creating new Google user: {}", email);

        User newUser = new User();
        newUser.setEmail(email.toLowerCase().trim());
        newUser.setName(name);
        newUser.setUsername(generateUniqueUsername(email));
        newUser.setProfilePictureUrl(picture);
        newUser.setIsVerified(true);
        newUser.setStatus(User.UserStatus.ACTIVE);
        newUser.setLanguage("en");
        newUser.setOauthProvider("google");
        newUser.setPassword("OAUTH2_USER_NO_PASSWORD");
        return repository.save(newUser);
    }

    private String generateUniqueUsername(String email) {
        String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
        String username = baseUsername;
        int suffix = 1;

        while (repository.findByUsername(username).isPresent()) {
            username = baseUsername + suffix;
            suffix++;
        }

        log.info("Generated unique username: {}", username);
        return username;
    }
}