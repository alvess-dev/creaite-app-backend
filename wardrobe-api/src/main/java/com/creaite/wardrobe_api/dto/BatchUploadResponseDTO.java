// wardrobe-api/src/main/java/com/creaite/wardrobe_api/dto/BatchUploadResponseDTO.java
package com.creaite.wardrobe_api.dto;

import java.util.List;
import java.util.UUID;

public record BatchUploadResponseDTO(
        List<UUID> clothingIds,
        String message,
        Integer totalUploaded
) {}