package com.creaite.wardrobe_api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BatchAdvancedUploadRequestDTO(
        @NotNull List<BatchAdvancedItemDTO> items,
        boolean processWithAI
) {}
