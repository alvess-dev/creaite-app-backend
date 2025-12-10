package com.creaite.wardrobe_api.infra.security.oauth;

import com.creaite.wardrobe_api.domain.user.User;
import com.creaite.wardrobe_api.dto.ResponseDTO;
import com.creaite.wardrobe_api.infra.security.TokenService;
import com.creaite.wardrobe_api.repositories.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository repository;
    private final TokenService tokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        log.info("=== OAuth2 Login Success ===");
        log.info("Email: {}", email);
        log.info("Name: {}", name);

        User user = this.repository.findByEmail(email).orElseGet(() -> createNewUser(email, name, picture));

        user.setLastLogin(LocalDateTime.now());
        this.repository.save(user);

        String token = tokenService.generateAccessToken(user);

        // ✅ CORRIGIDO: Incluir hasCompletedOnboarding
        ResponseDTO responseDTO = new ResponseDTO(
                user.getName(),
                token,
                user.getHasCompletedOnboarding() != null ? user.getHasCompletedOnboarding() : false
        );

        log.info("✅ OAuth2 authentication successful for user: {}", user.getEmail());
    }

    private User createNewUser(String email, String name, String picture) {
        log.info("=== Creating new OAuth2 user ===");

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setName(name);
        newUser.setUsername(generateUniqueUsername(email));
        newUser.setProfilePictureUrl(picture);
        newUser.setIsVerified(true);
        newUser.setStatus(User.UserStatus.ACTIVE);
        newUser.setLanguage("en");
        newUser.setOauthProvider("google");
        newUser.setPassword("OAUTH2_USER_NO_PASSWORD");

        // ✅ ADICIONADO: Novo usuário não completou onboarding
        newUser.setHasCompletedOnboarding(false);

        User savedUser = this.repository.save(newUser);
        log.info("✅ New OAuth2 user created with ID: {}", savedUser.getId());

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