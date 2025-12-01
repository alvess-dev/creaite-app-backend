// wardrobe-api/src/main/java/com/creaite/wardrobe_api/dto/BatchUploadRequestDTO.java
package com.creaite.wardrobe_api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BatchUploadRequestDTO(
        @NotEmpty(message = "At least one image is required")
        List<String> imagesBase64,

        @NotNull(message = "Process with AI flag is required")
        Boolean processWithAI
) {}