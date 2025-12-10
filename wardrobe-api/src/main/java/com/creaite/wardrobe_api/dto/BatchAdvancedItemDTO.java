package com.creaite.wardrobe_api.dto;

import com.creaite.wardrobe_api.domain.user.ClothingCategory;
import jakarta.validation.constraints.NotNull;

public record BatchAdvancedItemDTO(
        @NotNull String imageBase64,
        String name,
        ClothingCategory category,
        String color,
        String brand,
        String description,
        Boolean isPublic
) {}
