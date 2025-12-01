package com.creaite.wardrobe_api.controllers;

import com.creaite.wardrobe_api.domain.user.User;
import com.creaite.wardrobe_api.dto.*;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Value("${GOOGLE_CLIENT_ID}")
    private String googleClientId;

    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        log.info("=== CHECK EMAIL REQUEST ===");
        log.info("Endpoint: GET /auth/check-email");
        log.info("Email parameter: {}", email);

        try {
            if (email == null || email.trim().isEmpty()) {
                log.warn("Empty email provided");
                return ResponseEntity.badRequest()
                        .body(new ErrorResponseDTO("Email is required"));
            }

            String normalizedEmail = email.trim().toLowerCase();
            log.info("Normalized email: {}", normalizedEmail);

            Optional<User> existingUser = this.repository.findByEmail(normalizedEmail);
            log.info("User exists in database: {}", existingUser.isPresent());

            if (existingUser.isPresent()) {
                log.info("❌ Email {} already exists", normalizedEmail);
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErrorResponseDTO("Email already registered"));
            }

            log.info("✅ Email {} is available", normalizedEmail);
            return ResponseEntity.ok(Map.of("message", "Email available"));

        } catch (Exception e) {
            log.error("❌ Error checking email: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO("Error checking email", e.getMessage()));
        }
    }

    @GetMapping("/check-username")
    public ResponseEntity<?> checkUsername(@RequestParam String username) {
        log.info("=== CHECK USERNAME REQUEST ===");
        log.info("Endpoint: GET /auth/check-username");
        log.info("Username parameter: {}", username);

        try {
            if (username == null || username.trim().isEmpty()) {
                log.warn("Empty username provided");
                return ResponseEntity.badRequest()
                        .body(new ErrorResponseDTO("Username is required"));
            }

            String normalizedUsername = username.trim();
            log.info("Normalized username: {}", normalizedUsername);

            Optional<User> existingUser = this.repository.findByUsername(normalizedUsername);
            log.info("Username exists in database: {}", existingUser.isPresent());

            if (existingUser.isPresent()) {
                log.info("❌ Username {} already exists", normalizedUsername);
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErrorResponseDTO("Username already taken"));
            }

            log.info("✅ Username {} is available", normalizedUsername);
            return ResponseEntity.ok(Map.of("message", "Username available"));

        } catch (Exception e) {
            log.error("❌ Error checking username: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO("Error checking username", e.getMessage()));
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
                        .body(new ErrorResponseDTO("This account uses Google Sign-In. Please login with Google."));
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
                    .body(new ErrorResponseDTO("Invalid email or password"));

        } catch (RuntimeException e) {
            log.error("Login error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDTO(e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequestDTO body) {
        try {
            log.info("=== Registration attempt ===");
            log.info("Email: {}", body.email());
            log.info("Username: {}", body.username());
            log.info("Name: {}", body.name());
            log.info("BirthDate: {}", body.birthDate());
            log.info("Language: {}", body.language());

            // Validações detalhadas
            if (body.email() == null || body.email().trim().isEmpty()) {
                log.error("❌ Email is null or empty");
                return ResponseEntity.badRequest()
                        .body(new ErrorResponseDTO("Email is required"));
            }

            if (body.password() == null || body.password().trim().isEmpty()) {
                log.error("❌ Password is null or empty");
                return ResponseEntity.badRequest()
                        .body(new ErrorResponseDTO("Password is required"));
            }

            if (body.username() == null || body.username().trim().isEmpty()) {
                log.error("❌ Username is null or empty");
                return ResponseEntity.badRequest()
                        .body(new ErrorResponseDTO("Username is required"));
            }

            if (body.name() == null || body.name().trim().isEmpty()) {
                log.error("❌ Name is null or empty");
                return ResponseEntity.badRequest()
                        .body(new ErrorResponseDTO("Name is required"));
            }

            String normalizedEmail = body.email().toLowerCase().trim();

            Optional<User> existingUser = this.repository.findByEmail(normalizedEmail);
            if (existingUser.isPresent()) {
                log.warn("❌ Email already registered: {}", normalizedEmail);
                return ResponseEntity.badRequest()
                        .body(new ErrorResponseDTO("Email already registered"));
            }

            Optional<User> existingUsername = this.repository.findByUsername(body.username());
            if (existingUsername.isPresent()) {
                log.warn("❌ Username already taken: {}", body.username());
                return ResponseEntity.badRequest()
                        .body(new ErrorResponseDTO("Username already taken"));
            }

            User newUser = new User();
            newUser.setPassword(passwordEncoder.encode(body.password()));
            newUser.setEmail(normalizedEmail);
            newUser.setUsername(body.username().trim());
            newUser.setName(body.name().trim());

            // Parse birthDate se fornecido
            if (body.birthDate() != null && !body.birthDate().trim().isEmpty()) {
                try {
                    newUser.setBirthDate(LocalDate.parse(body.birthDate()));
                    log.info("BirthDate parsed: {}", newUser.getBirthDate());
                } catch (Exception e) {
                    log.warn("Failed to parse birthDate: {}", body.birthDate());
                    // Continua sem birthDate
                }
            }

            newUser.setLanguage(body.language() != null ? body.language() : "en");
            newUser.setStatus(User.UserStatus.ACTIVE);
            newUser.setIsVerified(false);

            log.info("Saving new user to database...");
            User savedUser = this.repository.save(newUser);
            log.info("✅ User saved successfully with ID: {}", savedUser.getId());

            String token = this.tokenService.generateAccessToken(savedUser);
            log.info("✅ Token generated successfully");

            ResponseDTO response = new ResponseDTO(savedUser.getName(), token);
            log.info("✅ Registration complete - returning response");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Registration error: {}", e.getMessage());
            log.error("Exception type: {}", e.getClass().getName());
            log.error("Stack trace: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO("Registration failed", e.getMessage()));
        }
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleAuth(@RequestBody GoogleTokenDTO body) {
        try {
            log.info("=== Google authentication attempt ===");

            if (body.idToken() == null || body.idToken().trim().isEmpty()) {
                log.error("❌ Empty or null idToken received");
                return ResponseEntity.badRequest()
                        .body(new ErrorResponseDTO("Invalid token: idToken is required"));
            }

            log.info("Received idToken (length: {})", body.idToken().length());

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            log.info("Verifying token with Google API...");

            GoogleIdToken idToken = null;
            try {
                idToken = verifier.verify(body.idToken());
            } catch (Exception verifyException) {
                log.error("❌ Token verification threw exception: {}", verifyException.getMessage());
                throw verifyException;
            }

            if (idToken == null) {
                log.error("❌ Token verification FAILED - Google returned null");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponseDTO("Invalid Google token",
                                "Token verification failed - token may be invalid or expired"));
            }

            log.info("✅ Token verified successfully by Google");
            GoogleIdToken.Payload payload = idToken.getPayload();

            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");

            log.info("Email: {}", email);
            log.info("Name: {}", name);

            if (email == null || email.trim().isEmpty()) {
                log.error("❌ Email is null or empty in token payload");
                return ResponseEntity.badRequest()
                        .body(new ErrorResponseDTO("Invalid token: email not found"));
            }

            if (name == null || name.trim().isEmpty()) {
                log.warn("⚠️ Name is null or empty in token, using email prefix");
                name = email.split("@")[0];
            }

            Optional<User> existingUserOpt = repository.findByEmail(email);

            User user;
            if (existingUserOpt.isPresent()) {
                log.info("✅ User found - existing Google user");
                user = existingUserOpt.get();

                if (picture != null && !picture.equals(user.getProfilePictureUrl())) {
                    user.setProfilePictureUrl(picture);
                }
                if (!name.equals(user.getName())) {
                    user.setName(name);
                }
            } else {
                log.info("User not found - creating new Google user");
                user = createGoogleUser(email, name, picture);
            }

            user.setLastLogin(LocalDateTime.now());
            repository.save(user);

            String token = tokenService.generateAccessToken(user);
            log.info("✅ Google Auth Complete");

            return ResponseEntity.ok(new ResponseDTO(user.getName(), token));

        } catch (Exception e) {
            log.error("❌ Google Auth Exception: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO("Google authentication failed", e.getMessage()));
        }
    }

    private User createGoogleUser(String email, String name, String picture) {
        log.info("=== Creating New Google User ===");

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required to create user");
        }

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required to create user");
        }

        User newUser = new User();
        newUser.setEmail(email.toLowerCase().trim());
        newUser.setName(name.trim());
        newUser.setUsername(generateUniqueUsername(email));
        newUser.setProfilePictureUrl(picture);
        newUser.setIsVerified(true);
        newUser.setStatus(User.UserStatus.ACTIVE);
        newUser.setLanguage("en");
        newUser.setOauthProvider("google");
        newUser.setPassword("OAUTH2_USER_NO_PASSWORD");

        User savedUser = repository.save(newUser);
        log.info("✅ Google user saved successfully with ID: {}", savedUser.getId());

        return savedUser;
    }

    private String generateUniqueUsername(String email) {
        String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");

        if (baseUsername.isEmpty()) {
            baseUsername = "user" + System.currentTimeMillis();
        }

        String username = baseUsername;
        int suffix = 1;

        while (repository.findByUsername(username).isPresent()) {
            username = baseUsername + suffix;
            suffix++;
        }

        log.info("Generated unique username: '{}'", username);
        return username;
    }
}