package com.creaite.wardrobe_api.dto;
import com.creaite.wardrobe_api.domain.user.User;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserDTO(
        String username,
        String name,
        LocalDate birthDate,
        String language,
        String profilePictureUrl,
        String bio,
        Boolean isVerified,
        User.UserStatus status,
        LocalDateTime lastLogin,
        String email,
        String oauthProvider
) {}
