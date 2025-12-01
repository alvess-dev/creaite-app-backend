// wardrobe-api/src/main/java/com/creaite/wardrobe_api/dto/ClothesDTO.java
package com.creaite.wardrobe_api.dto;

import com.creaite.wardrobe_api.domain.user.Clothes;
import com.creaite.wardrobe_api.domain.user.ClothingCategory;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClothesDTO(
        UUID id,
        String name,
        ClothingCategory category,
        String color,
        String brand,
        String clothingPictureUrl,
        String originalImageUrl,
        String description,
        Boolean isPublic,
        Clothes.ProcessingStatus processingStatus,
        String processingError,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}