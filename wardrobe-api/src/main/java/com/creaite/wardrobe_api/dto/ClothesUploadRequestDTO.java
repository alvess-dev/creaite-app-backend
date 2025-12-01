// wardrobe-api/src/main/java/com/creaite/wardrobe_api/dto/ClothesUploadRequestDTO.java
package com.creaite.wardrobe_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClothesUploadRequestDTO(
        @NotBlank(message = "Image data is required")
        String imageBase64,

        @NotNull(message = "Process with AI flag is required")
        Boolean processWithAI
) {}