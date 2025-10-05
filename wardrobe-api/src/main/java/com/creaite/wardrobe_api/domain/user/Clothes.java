package com.creaite.wardrobe_api.domain.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "clothing_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Clothes {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotBlank(message = "Name is required")
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank(message = "Category is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50, nullable = false)
    private ClothingCategory category;

    @NotBlank(message = "Color is required")
    @Column(name = "color", nullable = false)
    private String color;

    @Column(name = "brand")
    private String brand;

    @NotBlank(message = "picture is required")
    @Column(name = "image_url", nullable = false)
    private String clothingPictureUrl;

    @Column(name = "description")
    private String description;

    @Column(name = "is_public")
    private Boolean isPublic = true;
}