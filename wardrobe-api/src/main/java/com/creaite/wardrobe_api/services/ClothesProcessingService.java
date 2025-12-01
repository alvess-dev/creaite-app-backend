// wardrobe-api/src/main/java/com/creaite/wardrobe_api/services/ClothesProcessingService.java
package com.creaite.wardrobe_api.services;

import com.creaite.wardrobe_api.domain.user.Clothes;
import com.creaite.wardrobe_api.repositories.ClothesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesProcessingService {

    private final ClothesRepository clothesRepository;
    private final GeminiService geminiService;

    @Async
    public void processClothingImageAsync(UUID clothingId) {
        try {
            log.info("=== Starting async processing for clothing ID: {} ===", clothingId);

            Clothes clothing = clothesRepository.findById(clothingId)
                    .orElseThrow(() -> new RuntimeException("Clothing not found"));

            // Atualiza status para PROCESSING
            clothing.setProcessingStatus(Clothes.ProcessingStatus.PROCESSING);
            clothesRepository.save(clothing);
            log.info("Status updated to PROCESSING");

            // Processa a imagem com Gemini
            String originalImage = clothing.getOriginalImageUrl();
            log.info("Processing image with Gemini...");

            String processedImage = geminiService.processImageWithGemini(originalImage);
            log.info("✅ Image processed successfully");

            // Atualiza a roupa com a imagem processada
            clothing.setClothingPictureUrl(processedImage);
            clothing.setProcessingStatus(Clothes.ProcessingStatus.COMPLETED);
            clothing.setProcessingError(null);
            clothesRepository.save(clothing);

            log.info("✅ Clothing {} processing completed successfully", clothingId);

        } catch (Exception e) {
            log.error("❌ Error processing clothing {}: {}", clothingId, e.getMessage(), e);

            // Atualiza status para FAILED
            clothesRepository.findById(clothingId).ifPresent(clothing -> {
                clothing.setProcessingStatus(Clothes.ProcessingStatus.FAILED);
                clothing.setProcessingError(e.getMessage());
                clothesRepository.save(clothing);
            });
        }
    }

    @Async
    public void processBatchClothingImagesAsync(Iterable<UUID> clothingIds) {
        log.info("=== Starting batch processing ===");

        for (UUID clothingId : clothingIds) {
            try {
                processClothingImageAsync(clothingId);

                // Pequeno delay entre processamentos para não sobrecarregar a API
                Thread.sleep(1000);

            } catch (Exception e) {
                log.error("❌ Error in batch processing for clothing {}: {}", clothingId, e.getMessage());
            }
        }

        log.info("✅ Batch processing completed");
    }
}