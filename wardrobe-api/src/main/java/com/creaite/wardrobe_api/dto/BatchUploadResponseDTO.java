// wardrobe-api/src/main/java/com/creaite/wardrobe_api/dto/BatchUploadResponseDTO.java
package com.creaite.wardrobe_api.dto;

import java.util.List;

public record BatchUploadResponseDTO(
        List<String> clothingIds,  // ✅ Mudou de UUID para String
        String message,
        Integer totalUploaded
) {}