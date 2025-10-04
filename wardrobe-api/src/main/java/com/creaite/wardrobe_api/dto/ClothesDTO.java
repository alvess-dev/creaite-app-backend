package com.creaite.wardrobe_api.dto;

public record ClothesDTO(
        String name,
        String category,
        String color,
        String brand,
        String clothingPictureUrl,
        String description,
        Boolean isPublic
) {}
