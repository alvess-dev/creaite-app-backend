// wardrobe-api/src/main/java/com/creaite/wardrobe_api/controllers/ClothesController.java
package com.creaite.wardrobe_api.controllers;

import com.creaite.wardrobe_api.domain.user.Clothes;
import com.creaite.wardrobe_api.domain.user.User;
import com.creaite.wardrobe_api.dto.*;
import com.creaite.wardrobe_api.repositories.ClothesRepository;
import com.creaite.wardrobe_api.repositories.UserRepository;
import com.creaite.wardrobe_api.services.ClothesProcessingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/clothes")
@RequiredArgsConstructor
public class ClothesController {

    private final UserRepository userRepository;
    private final ClothesRepository clothesRepository;
    private final ClothesProcessingService processingService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadClothing(
            @AuthenticationPrincipal User userBody,
            @RequestBody @Valid ClothesUploadRequestDTO body) {
        try {
            log.info("=== Upload Clothing Request ===");
            log.info("Process with AI: {}", body.processWithAI());

            User user = userRepository.findByEmail(userBody.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Clothes newClothing = new Clothes();
            newClothing.setUserId(user.getId());
            newClothing.setOriginalImageUrl(body.imageBase64());

            if (body.processWithAI()) {
                // Se processar com IA, começa com PENDING
                newClothing.setProcessingStatus(Clothes.ProcessingStatus.PENDING);
                newClothing.setClothingPictureUrl(body.imageBase64()); // Temporário
            } else {
                // Se não processar, já marca como COMPLETED
                newClothing.setProcessingStatus(Clothes.ProcessingStatus.COMPLETED);
                newClothing.setClothingPictureUrl(body.imageBase64());
            }

            // Valores padrão
            newClothing.setName("New Item");
            newClothing.setIsPublic(true);

            Clothes saved = clothesRepository.save(newClothing);
            log.info("✅ Clothing saved with ID: {}", saved.getId());

            // Se processar com IA, inicia processamento assíncrono
            if (body.processWithAI()) {
                log.info("Starting async AI processing...");
                processingService.processClothingImageAsync(saved.getId());
            }

            return ResponseEntity.ok(convertToDTO(saved));

        } catch (Exception e) {
            log.error("❌ Upload error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDTO("Upload failed", e.getMessage()));
        }
    }

    @PostMapping("/upload/batch")
    public ResponseEntity<?> uploadBatchClothing(
            @AuthenticationPrincipal User userBody,
            @RequestBody @Valid BatchUploadRequestDTO body) {
        try {
            log.info("=== Batch Upload Request ===");
            log.info("Number of images: {}", body.imagesBase64().size());
            log.info("Process with AI: {}", body.processWithAI());

            User user = userRepository.findByEmail(userBody.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<UUID> clothingIds = new ArrayList<>();

            for (String imageBase64 : body.imagesBase64()) {
                Clothes newClothing = new Clothes();
                newClothing.setUserId(user.getId());
                newClothing.setOriginalImageUrl(imageBase64);

                if (body.processWithAI()) {
                    newClothing.setProcessingStatus(Clothes.ProcessingStatus.PENDING);
                    newClothing.setClothingPictureUrl(imageBase64);
                } else {
                    newClothing.setProcessingStatus(Clothes.ProcessingStatus.COMPLETED);
                    newClothing.setClothingPictureUrl(imageBase64);
                }

                newClothing.setName("New Item");
                newClothing.setIsPublic(true);

                Clothes saved = clothesRepository.save(newClothing);
                clothingIds.add(saved.getId());
            }

            log.info("✅ {} items saved", clothingIds.size());

            // Se processar com IA, inicia processamento em batch
            if (body.processWithAI()) {
                log.info("Starting batch async AI processing...");
                processingService.processBatchClothingImagesAsync(clothingIds);
            }

            return ResponseEntity.ok(new BatchUploadResponseDTO(
                    clothingIds,
                    "Upload successful",
                    clothingIds.size()
            ));

        } catch (Exception e) {
            log.error("❌ Batch upload error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDTO("Batch upload failed", e.getMessage()));
        }
    }

    @GetMapping("/status/{id}")
    public ResponseEntity<?> getClothingStatus(
            @AuthenticationPrincipal User userBody,
            @PathVariable UUID id) {
        try {
            User user = userRepository.findByEmail(userBody.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Clothes clothing = clothesRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Clothing not found"));

            if (!clothing.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body("Access denied");
            }

            return ResponseEntity.ok(convertToDTO(clothing));

        } catch (Exception e) {
            log.error("❌ Get status error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDTO("Failed to get status", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @AuthenticationPrincipal User userBody,
            @PathVariable UUID id) {
        try {
            User user = userRepository.findByEmail(userBody.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Optional<Clothes> clothesOpt = clothesRepository.findById(id);

            if (clothesOpt.isEmpty() || !clothesOpt.get().getUserId().equals(user.getId())) {
                return ResponseEntity.status(404).body("Clothing item not found for this user");
            }

            clothesRepository.delete(clothesOpt.get());
            return ResponseEntity.ok("Clothing item deleted successfully");

        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<?> patchClothing(
            @AuthenticationPrincipal User userBody,
            @RequestBody @Valid ClothesDTO body,
            @PathVariable UUID id) {
        try {
            User user = userRepository.findByEmail(userBody.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Optional<Clothes> clothingOpt = clothesRepository.findById(id);

            if (clothingOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Clothes clothing = clothingOpt.get();

            if (!clothing.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body(null);
            }

            if (body.name() != null) clothing.setName(body.name());
            if (body.category() != null) clothing.setCategory(body.category());
            if (body.color() != null) clothing.setColor(body.color());
            if (body.brand() != null) clothing.setBrand(body.brand());
            if (body.clothingPictureUrl() != null) clothing.setClothingPictureUrl(body.clothingPictureUrl());
            if (body.description() != null) clothing.setDescription(body.description());
            if (body.isPublic() != null) clothing.setIsPublic(body.isPublic());

            clothesRepository.save(clothing);

            return ResponseEntity.ok(convertToDTO(clothing));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(
            @AuthenticationPrincipal User userBody,
            @RequestBody @Valid ClothesDTO body) {
        try {
            User user = userRepository.findByEmail(userBody.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Clothes newClothing = new Clothes();

            newClothing.setUserId(user.getId());
            newClothing.setName(body.name());
            newClothing.setCategory(body.category());
            newClothing.setColor(body.color());
            newClothing.setBrand(body.brand());
            newClothing.setClothingPictureUrl(body.clothingPictureUrl());
            newClothing.setDescription(body.description());
            newClothing.setIsPublic(body.isPublic());
            newClothing.setProcessingStatus(Clothes.ProcessingStatus.COMPLETED);

            clothesRepository.save(newClothing);

            return ResponseEntity.ok(convertToDTO(newClothing));

        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    private ClothesDTO convertToDTO(Clothes clothing) {
        return new ClothesDTO(
                clothing.getId(),
                clothing.getName(),
                clothing.getCategory(),
                clothing.getColor(),
                clothing.getBrand(),
                clothing.getClothingPictureUrl(),
                clothing.getOriginalImageUrl(),
                clothing.getDescription(),
                clothing.getIsPublic(),
                clothing.getProcessingStatus(),
                clothing.getProcessingError(),
                clothing.getCreatedAt(),
                clothing.getUpdatedAt()
        );
    }
}