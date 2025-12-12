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
    private final OpenAIService openAIService;
    private final RemoveBGService removeBGService;

    /**
     * Processa a imagem de forma assíncrona
     * @param clothingId ID da roupa
     * @param enhanceWithAI Se deve processar com IA
     */
    @Async("taskExecutor")
    public void processClothingImageAsync(UUID clothingId, boolean enhanceWithAI) {
        try {
            log.info("=== Starting async processing for clothing ID: {} ===", clothingId);
            log.info("Enhance with AI: {}", enhanceWithAI);

            Clothes clothing = clothesRepository.findById(clothingId)
                    .orElseThrow(() -> new RuntimeException("Clothing not found"));

            String processedImage = clothing.getOriginalImageUrl();

            // Etapa 1: Processar com IA (se solicitado)
            if (enhanceWithAI) {
                log.info("Step 1/2: Processing with AI...");
                clothing.setProcessingStatus(Clothes.ProcessingStatus.PROCESSING_AI);
                clothesRepository.save(clothing);

                try {
                    processedImage = openAIService.enhanceImageWithAI(processedImage);
                    log.info("✅ AI enhancement complete");
                } catch (Exception e) {
                    log.error("❌ AI enhancement failed: {}", e.getMessage());
                    // Continua com a imagem original
                }
            }

            // Etapa 2: Remover fundo (sempre)
            log.info("Step {}/2: Removing background...", enhanceWithAI ? 2 : 1);
            clothing.setProcessingStatus(Clothes.ProcessingStatus.REMOVING_BACKGROUND);
            clothesRepository.save(clothing);

            try {
                processedImage = removeBGService.removeBackground(processedImage);
                log.info("✅ Background removal complete");
            } catch (Exception e) {
                log.error("❌ Background removal failed: {}", e.getMessage());
                // Continua com a imagem que tem (com ou sem IA)
            }

            // Finaliza o processamento
            clothing.setClothingPictureUrl(processedImage);
            clothing.setProcessingStatus(Clothes.ProcessingStatus.COMPLETED);
            clothing.setProcessingError(null);
            clothesRepository.save(clothing);

            log.info("✅ Clothing {} processing completed successfully", clothingId);

        } catch (Exception e) {
            log.error("❌ Error processing clothing {}: {}", clothingId, e.getMessage(), e);

            clothesRepository.findById(clothingId).ifPresent(clothing -> {
                clothing.setProcessingStatus(Clothes.ProcessingStatus.FAILED);
                clothing.setProcessingError(e.getMessage());
                clothesRepository.save(clothing);
            });
        }
    }

    /**
     * Processa múltiplas imagens em batch
     */
    @Async("taskExecutor")
    public void processBatchClothingImagesAsync(Iterable<UUID> clothingIds, boolean enhanceWithAI) {
        log.info("=== Starting batch processing ===");
        log.info("Enhance with AI: {}", enhanceWithAI);

        for (UUID clothingId : clothingIds) {
            try {
                processClothingImageAsync(clothingId, enhanceWithAI);

                // Pequeno delay entre processamentos
                Thread.sleep(2000);

            } catch (Exception e) {
                log.error("❌ Error in batch processing for clothing {}: {}", clothingId, e.getMessage());
            }
        }

        log.info("✅ Batch processing completed");
    }
}