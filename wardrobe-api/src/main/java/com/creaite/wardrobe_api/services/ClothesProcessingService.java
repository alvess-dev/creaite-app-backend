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
            log.info("=== Starting async simulated processing for clothing ID: {} ===", clothingId);

            Clothes clothing = clothesRepository.findById(clothingId)
                    .orElseThrow(() -> new RuntimeException("Clothing not found"));

            // Atualiza status para PROCESSING
            clothing.setProcessingStatus(Clothes.ProcessingStatus.PROCESSING);
            clothesRepository.save(clothing);
            log.info("Status updated to PROCESSING for {}", clothingId);

            // Chama GeminiService (que agora sempre carrega camiseta.jpeg)
            String processedImage = geminiService.processImageWithGemini(clothing.getOriginalImageUrl());

            // Atualiza a roupa com a imagem processada
            clothing.setClothingPictureUrl(processedImage);
            clothing.setProcessingStatus(Clothes.ProcessingStatus.COMPLETED);
            clothing.setProcessingError(null);
            clothesRepository.save(clothing);

            log.info("✅ Clothing {} simulated processing completed successfully", clothingId);

        } catch (Exception e) {
            log.error("❌ Error in simulated processing for clothing {}: {}", clothingId, e.getMessage(), e);

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
        log.info("=== Starting batch simulated processing ===");

        for (UUID clothingId : clothingIds) {
            try {
                processClothingImageAsync(clothingId);

                // Pequeno delay entre processamentos
                Thread.sleep(1000);

            } catch (Exception e) {
                log.error("❌ Error in batch simulated processing for clothing {}: {}", clothingId, e.getMessage());
            }
        }

        log.info("✅ Batch simulated processing completed");
    }
}
