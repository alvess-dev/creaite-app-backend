package com.creaite.wardrobe_api.dto;

import com.creaite.wardrobe_api.domain.user.ClothingCategory;

public record ClothesDTO(
        String name,
        ClothingCategory category,
        String color,
        String brand,
        String clothingPictureUrl,
        String description,
        Boolean isPublic
) {}
