package com.creaite.wardrobe_api.domain.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Past(message = "Birth date cannot be in the future")
    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private UserStatus status;

    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @NotBlank(message = "Language is required")
    @Column(name = "language", length = 10)
    private String language = "en";

    @NotBlank(message = "Name is required")
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank(message = "Username is required")
    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Email(message = "Invalid Email")
    @NotBlank(message = "Email is required")
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 12, max = 64, message = "Password must be between 12 and 64 characters")
    @Column(name = "password_hash", nullable = false)
    private String password;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Column(name = "bio")
    private String bio;

    public enum UserStatus {
        ACTIVE,
        INACTIVE,
        BANNED
    }
}
