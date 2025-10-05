package com.creaite.wardrobe_api.dto;

import com.creaite.wardrobe_api.domain.user.ClothingCategory;

import java.util.UUID;

public record ClothesDTO(
        UUID id,
        String name,
        ClothingCategory category,
        String color,
        String brand,
        String clothingPictureUrl,
        String description,
        Boolean isPublic
) {}
