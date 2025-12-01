package com.creaite.wardrobe_api.dto;

public record ErrorResponseDTO(String error, String details) {
    public ErrorResponseDTO(String error) {
        this(error, null);
    }
}