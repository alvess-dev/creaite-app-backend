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
                        .body(Map.of("error", "Email is required"));
            }

            String normalizedEmail = email.trim().toLowerCase();
            log.info("Normalized email: {}", normalizedEmail);

            Optional<User> existingUser = this.repository.findByEmail(normalizedEmail);
            log.info("User exists in database: {}", existingUser.isPresent());

            if (existingUser.isPresent()) {
                log.info("❌ Email {} already exists", normalizedEmail);
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Email already registered"));
            }

            log.info("✅ Email {} is available", normalizedEmail);
            return ResponseEntity.ok(Map.of("message", "Email available"));

        } catch (Exception e) {
            log.error("❌ Error checking email: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error checking email: " + e.getMessage()));
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
                        .body(Map.of("error", "Username is required"));
            }

            String normalizedUsername = username.trim();
            log.info("Normalized username: {}", normalizedUsername);

            Optional<User> existingUser = this.repository.findByUsername(normalizedUsername);
            log.info("Username exists in database: {}", existingUser.isPresent());

            if (existingUser.isPresent()) {
                log.info("❌ Username {} already exists", normalizedUsername);
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Username already taken"));
            }

            log.info("✅ Username {} is available", normalizedUsername);
            return ResponseEntity.ok(Map.of("message", "Username available"));

        } catch (Exception e) {
            log.error("❌ Error checking username: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error checking username: " + e.getMessage()));
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

            // 1. Validar se o token foi enviado
            if (body.idToken() == null || body.idToken().trim().isEmpty()) {
                log.error("❌ Empty or null idToken received");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid token: idToken is required"));
            }

            log.info("Received idToken (length: {})", body.idToken().length());
            log.info("Token preview: {}...", body.idToken().substring(0, Math.min(50, body.idToken().length())));

            // 2. Configurar verificador do Google
            log.info("Configuring Google Token Verifier...");
            log.info("Using Client ID (first 30 chars): {}...",
                    googleClientId.substring(0, Math.min(30, googleClientId.length())));

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            log.info("Verifying token with Google API...");

            // 3. Verificar o token
            GoogleIdToken idToken = null;
            try {
                idToken = verifier.verify(body.idToken());
            } catch (Exception verifyException) {
                log.error("❌ Token verification threw exception");
                log.error("Exception type: {}", verifyException.getClass().getName());
                log.error("Exception message: {}", verifyException.getMessage());
                throw verifyException;
            }

            // 4. Verificar se a verificação falhou
            if (idToken == null) {
                log.error("❌ Token verification FAILED - Google returned null");
                log.error("Possible causes:");
                log.error("  1. Wrong Client ID configured in backend");
                log.error("  2. Token was generated for a different Client ID");
                log.error("  3. Token has expired");
                log.error("  4. Token signature is invalid");
                log.error("");
                log.error("Backend is using Client ID: {}...",
                        googleClientId.substring(0, Math.min(30, googleClientId.length())));

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "error", "Invalid Google token",
                                "details", "Token verification failed - token may be invalid or expired"
                        ));
            }

            // 5. Extrair dados do payload
            log.info("✅ Token verified successfully by Google");
            GoogleIdToken.Payload payload = idToken.getPayload();

            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");
            Boolean emailVerified = payload.getEmailVerified();
            String subject = payload.getSubject(); // Google User ID

            log.info("=== Token Payload Data ===");
            log.info("Subject (Google User ID): {}", subject);
            log.info("Email: {}", email);
            log.info("Email Verified: {}", emailVerified);
            log.info("Name: {}", name);
            log.info("Picture URL: {}", picture != null ? "present" : "null");

            // 6. Validar dados obrigatórios do payload
            if (email == null || email.trim().isEmpty()) {
                log.error("❌ Email is null or empty in token payload");
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error", "Invalid token: email not found",
                                "details", "Google token did not contain email address"
                        ));
            }

            // 7. Garantir que name não seja vazio
            if (name == null || name.trim().isEmpty()) {
                log.warn("⚠️ Name is null or empty in token, using email prefix");
                name = email.split("@")[0];
                log.info("Generated name from email: {}", name);
            }

            // 8. Buscar ou criar usuário
            log.info("Looking up user by email: {}", email);
            Optional<User> existingUserOpt = repository.findByEmail(email);

            User user;
            if (existingUserOpt.isPresent()) {
                log.info("✅ User found - existing Google user");
                user = existingUserOpt.get();

                // Atualizar informações se mudaram
                boolean updated = false;
                if (picture != null && !picture.equals(user.getProfilePictureUrl())) {
                    log.info("Updating profile picture");
                    user.setProfilePictureUrl(picture);
                    updated = true;
                }
                if (!name.equals(user.getName())) {
                    log.info("Updating name from '{}' to '{}'", user.getName(), name);
                    user.setName(name);
                    updated = true;
                }

                if (updated) {
                    log.info("Saving updated user info");
                }
            } else {
                log.info("User not found - creating new Google user");
                user = createGoogleUser(email, name, picture);
            }

            // 9. Atualizar último login
            user.setLastLogin(LocalDateTime.now());
            repository.save(user);
            log.info("User last login updated");

            // 10. Gerar token JWT
            log.info("Generating JWT access token...");
            String token = tokenService.generateAccessToken(user);

            log.info("=== Google Auth Complete ===");
            log.info("User ID: {}", user.getId());
            log.info("Username: {}", user.getUsername());
            log.info("Email: {}", user.getEmail());
            log.info("✅ JWT token generated successfully");

            return ResponseEntity.ok(new ResponseDTO(user.getName(), token));

        } catch (Exception e) {
            log.error("=== Google Auth Exception ===");
            log.error("Exception class: {}", e.getClass().getName());
            log.error("Exception message: {}", e.getMessage());
            log.error("Stack trace:", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Google authentication failed",
                            "details", e.getMessage() != null ? e.getMessage() : "Unknown error",
                            "type", e.getClass().getSimpleName()
                    ));
        }
    }

    private User createGoogleUser(String email, String name, String picture) {
        log.info("=== Creating New Google User ===");
        log.info("Email: '{}'", email);
        log.info("Name: '{}'", name);
        log.info("Picture: '{}'", picture);

        // Validações de segurança
        if (email == null || email.trim().isEmpty()) {
            log.error("❌ FATAL: Cannot create user - email is null or empty");
            throw new IllegalArgumentException("Email is required to create user");
        }

        if (name == null || name.trim().isEmpty()) {
            log.error("❌ FATAL: Cannot create user - name is null or empty");
            throw new IllegalArgumentException("Name is required to create user");
        }

        try {
            User newUser = new User();
            newUser.setEmail(email.toLowerCase().trim());
            newUser.setName(name.trim());
            newUser.setUsername(generateUniqueUsername(email));
            newUser.setProfilePictureUrl(picture);
            newUser.setIsVerified(true); // Google já verificou o email
            newUser.setStatus(User.UserStatus.ACTIVE);
            newUser.setLanguage("en");
            newUser.setOauthProvider("google");
            newUser.setPassword("OAUTH2_USER_NO_PASSWORD");

            log.info("User object created, attempting to save to database...");
            log.info("Username: {}", newUser.getUsername());
            log.info("OAuth Provider: {}", newUser.getOauthProvider());
            log.info("Is Verified: {}", newUser.getIsVerified());

            User savedUser = repository.save(newUser);

            log.info("✅ Google user saved successfully");
            log.info("Generated User ID: {}", savedUser.getId());
            log.info("Final Username: {}", savedUser.getUsername());

            return savedUser;

        } catch (Exception e) {
            log.error("❌ Failed to save Google user to database");
            log.error("Error type: {}", e.getClass().getName());
            log.error("Error message: {}", e.getMessage());
            log.error("Stack trace:", e);
            throw new RuntimeException("Failed to create Google user: " + e.getMessage(), e);
        }
    }

    private String generateUniqueUsername(String email) {
        log.info("Generating unique username from email: {}", email);

        String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");

        // Garantir que não seja vazio
        if (baseUsername.isEmpty()) {
            log.warn("Email resulted in empty username, using default");
            baseUsername = "user" + System.currentTimeMillis();
        }

        String username = baseUsername;
        int suffix = 1;
        int attempts = 0;
        int maxAttempts = 1000;

        while (repository.findByUsername(username).isPresent()) {
            username = baseUsername + suffix;
            suffix++;
            attempts++;

            if (attempts >= maxAttempts) {
                log.warn("Username generation exceeded max attempts, using timestamp");
                username = baseUsername + "_" + System.currentTimeMillis();
                break;
            }
        }

        log.info("Generated unique username: '{}' (took {} attempts)", username, attempts);
        return username;
    }
}