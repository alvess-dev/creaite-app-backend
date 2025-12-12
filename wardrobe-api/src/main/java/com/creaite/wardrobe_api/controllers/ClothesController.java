package com.creaite.wardrobe_api.controllers;

import com.creaite.wardrobe_api.domain.user.Clothes;
import com.creaite.wardrobe_api.domain.user.ClothingCategory;
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
                newClothing.setProcessingStatus(Clothes.ProcessingStatus.PENDING);
                newClothing.setClothingPictureUrl(body.imageBase64());
            } else {
                newClothing.setProcessingStatus(Clothes.ProcessingStatus.COMPLETED);
                newClothing.setClothingPictureUrl(body.imageBase64());
            }

            newClothing.setName("New Item");
            newClothing.setCategory(ClothingCategory.SHIRT);
            newClothing.setColor("Unknown");
            newClothing.setBrand("Unknown");
            newClothing.setIsPublic(true);
            newClothing.setIsFavorite(false);  // ✅ NOVO

            Clothes saved = clothesRepository.save(newClothing);
            log.info("✅ Clothing saved with ID: {}", saved.getId());

            if (body.processWithAI()) {
                log.info("Starting async AI processing (simulated)...");
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
                newClothing.setCategory(ClothingCategory.SHIRT);
                newClothing.setColor("Unknown");
                newClothing.setBrand("Unknown");
                newClothing.setIsPublic(true);
                newClothing.setIsFavorite(false);  // ✅ NOVO

                Clothes saved = clothesRepository.save(newClothing);
                clothingIds.add(saved.getId());
            }

            log.info("✅ {} items saved", clothingIds.size());

            if (body.processWithAI()) {
                log.info("Starting batch async AI processing (simulated)...");
                processingService.processBatchClothingImagesAsync(clothingIds);
            }

            List<String> clothingIdsAsStrings = clothingIds.stream()
                    .map(UUID::toString)
                    .toList();

            return ResponseEntity.ok(new BatchUploadResponseDTO(
                    clothingIdsAsStrings,
                    "Upload successful",
                    clothingIds.size()
            ));

        } catch (Exception e) {
            log.error("❌ Batch upload error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDTO("Batch upload failed", e.getMessage()));
        }
    }

    // ✅ NOVO: Upload avançado com metadados customizados
    @PostMapping("/upload/advanced")
    public ResponseEntity<?> uploadAdvancedClothing(
            @AuthenticationPrincipal User userBody,
            @RequestBody @Valid BatchAdvancedItemDTO body) {
        try {
            log.info("=== Advanced Upload Request ===");
            log.info("Name: {}", body.name());
            log.info("Category: {}", body.category());

            User user = userRepository.findByEmail(userBody.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Clothes newClothing = new Clothes();
            newClothing.setUserId(user.getId());
            newClothing.setOriginalImageUrl(body.imageBase64());
            newClothing.setClothingPictureUrl(body.imageBase64());

            // Metadados customizados
            newClothing.setName(body.name() != null ? body.name() : "New Item");
            newClothing.setCategory(body.category() != null ? body.category() : ClothingCategory.SHIRT);
            newClothing.setColor(body.color() != null ? body.color() : "Unknown");
            newClothing.setBrand(body.brand() != null ? body.brand() : "Unknown");
            newClothing.setDescription(body.description());
            newClothing.setIsPublic(body.isPublic() != null ? body.isPublic() : true);
            newClothing.setIsFavorite(false);
            newClothing.setProcessingStatus(Clothes.ProcessingStatus.COMPLETED);

            Clothes saved = clothesRepository.save(newClothing);
            log.info("✅ Advanced clothing saved with ID: {}", saved.getId());

            return ResponseEntity.ok(convertToDTO(saved));

        } catch (Exception e) {
            log.error("❌ Advanced upload error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDTO("Advanced upload failed", e.getMessage()));
        }
    }

    // ✅ NOVO: Toggle favorito
    @PatchMapping("/{id}/favorite")
    public ResponseEntity<?> toggleFavorite(
            @AuthenticationPrincipal User userBody,
            @PathVariable UUID id) {
        try {
            log.info("=== Toggle Favorite ===");
            log.info("Clothing ID: {}", id);

            User user = userRepository.findByEmail(userBody.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Clothes clothing = clothesRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Clothing not found"));

            if (!clothing.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body("Access denied");
            }

            clothing.setIsFavorite(!clothing.getIsFavorite());
            clothesRepository.save(clothing);

            log.info("✅ Favorite toggled: {}", clothing.getIsFavorite());
            return ResponseEntity.ok(convertToDTO(clothing));

        } catch (Exception e) {
            log.error("❌ Toggle favorite error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDTO("Failed to toggle favorite", e.getMessage()));
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

    @PostMapping("/upload/batch-advanced")
    public ResponseEntity<?> uploadBatchAdvanced(
            @AuthenticationPrincipal User userBody,
            @RequestBody @Valid BatchAdvancedUploadRequestDTO body) {
        try {
            log.info("=== Batch Advanced Upload Request ===");
            log.info("Number of items: {}", body.items().size());
            log.info("Process with AI: {}", body.processWithAI());

            User user = userRepository.findByEmail(userBody.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<UUID> clothingIds = new ArrayList<>();

            for (BatchAdvancedItemDTO item : body.items()) {
                Clothes clothing = new Clothes();
                clothing.setUserId(user.getId());
                clothing.setOriginalImageUrl(item.imageBase64());

                if (body.processWithAI()) {
                    clothing.setProcessingStatus(Clothes.ProcessingStatus.PENDING);
                    clothing.setClothingPictureUrl(item.imageBase64());
                } else {
                    clothing.setProcessingStatus(Clothes.ProcessingStatus.COMPLETED);
                    clothing.setClothingPictureUrl(item.imageBase64());
                }

                clothing.setName(item.name() != null ? item.name() : "New Item");
                clothing.setCategory(item.category() != null ? item.category() : ClothingCategory.SHIRT);
                clothing.setColor(item.color() != null ? item.color() : "Unknown");
                clothing.setBrand(item.brand() != null ? item.brand() : "Unknown");
                clothing.setDescription(item.description());
                clothing.setIsPublic(item.isPublic() != null ? item.isPublic() : true);
                clothing.setIsFavorite(false);

                Clothes saved = clothesRepository.save(clothing);
                clothingIds.add(saved.getId());
            }

            log.info("Saved {} advanced items", clothingIds.size());

            if (body.processWithAI()) {
                log.info("Starting batch-advanced async AI processing...");
                processingService.processBatchClothingImagesAsync(clothingIds);
            }

            List<String> clothingIdsAsStrings = clothingIds.stream()
                    .map(UUID::toString)
                    .toList();

            return ResponseEntity.ok(new BatchUploadResponseDTO(
                    clothingIdsAsStrings,
                    "Advanced batch upload successful",
                    clothingIds.size()
            ));

        } catch (Exception e) {
            log.error("❌ Batch advanced upload error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDTO("Batch advanced upload failed", e.getMessage()));
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
            newClothing.setName(body.name() != null ? body.name() : "New Item");
            newClothing.setCategory(body.category() != null ? body.category() : ClothingCategory.SHIRT);
            newClothing.setColor(body.color() != null ? body.color() : "Unknown");
            newClothing.setBrand(body.brand() != null ? body.brand() : "Unknown");
            newClothing.setClothingPictureUrl(body.clothingPictureUrl());
            newClothing.setDescription(body.description());
            newClothing.setIsPublic(body.isPublic() != null ? body.isPublic() : true);
            newClothing.setIsFavorite(false);
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
                clothing.getIsFavorite(),  // ✅ NOVO
                clothing.getProcessingStatus(),
                clothing.getProcessingError(),
                clothing.getCreatedAt(),
                clothing.getUpdatedAt()
        );
    }

    // Atualize estes métodos no ClothesController.java

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
            newClothing.setClothingPictureUrl(body.imageBase64()); // Temporário
            newClothing.setProcessingStatus(Clothes.ProcessingStatus.PENDING);
            newClothing.setName("New Item");
            newClothing.setCategory(ClothingCategory.SHIRT);
            newClothing.setColor("Unknown");
            newClothing.setBrand("Unknown");
            newClothing.setIsPublic(true);
            newClothing.setIsFavorite(false);

            Clothes saved = clothesRepository.save(newClothing);
            log.info("✅ Clothing saved with ID: {}", saved.getId());

            // Inicia processamento assíncrono
            log.info("Starting async processing (AI: {})...", body.processWithAI());
            processingService.processClothingImageAsync(saved.getId(), body.processWithAI());

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
                newClothing.setClothingPictureUrl(imageBase64); // Temporário
                newClothing.setProcessingStatus(Clothes.ProcessingStatus.PENDING);
                newClothing.setName("New Item");
                newClothing.setCategory(ClothingCategory.SHIRT);
                newClothing.setColor("Unknown");
                newClothing.setBrand("Unknown");
                newClothing.setIsPublic(true);
                newClothing.setIsFavorite(false);

                Clothes saved = clothesRepository.save(newClothing);
                clothingIds.add(saved.getId());
            }

            log.info("✅ {} items saved", clothingIds.size());

            // Inicia processamento em batch
            log.info("Starting batch async processing (AI: {})...", body.processWithAI());
            processingService.processBatchClothingImagesAsync(clothingIds, body.processWithAI());

            List<String> clothingIdsAsStrings = clothingIds.stream()
                    .map(UUID::toString)
                    .toList();

            return ResponseEntity.ok(new BatchUploadResponseDTO(
                    clothingIdsAsStrings,
                    "Upload successful - processing in background",
                    clothingIds.size()
            ));

        } catch (Exception e) {
            log.error("❌ Batch upload error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDTO("Batch upload failed", e.getMessage()));
        }
    }

    @PostMapping("/upload/batch-advanced")
    public ResponseEntity<?> uploadBatchAdvanced(
            @AuthenticationPrincipal User userBody,
            @RequestBody @Valid BatchAdvancedUploadRequestDTO body) {
        try {
            log.info("=== Batch Advanced Upload Request ===");
            log.info("Number of items: {}", body.items().size());
            log.info("Process with AI: {}", body.processWithAI());

            User user = userRepository.findByEmail(userBody.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<UUID> clothingIds = new ArrayList<>();

            for (BatchAdvancedItemDTO item : body.items()) {
                Clothes clothing = new Clothes();
                clothing.setUserId(user.getId());
                clothing.setOriginalImageUrl(item.imageBase64());
                clothing.setClothingPictureUrl(item.imageBase64()); // Temporário
                clothing.setProcessingStatus(Clothes.ProcessingStatus.PENDING);
                clothing.setName(item.name() != null ? item.name() : "New Item");
                clothing.setCategory(item.category() != null ? item.category() : ClothingCategory.SHIRT);
                clothing.setColor(item.color() != null ? item.color() : "Unknown");
                clothing.setBrand(item.brand() != null ? item.brand() : "Unknown");
                clothing.setDescription(item.description());
                clothing.setIsPublic(item.isPublic() != null ? item.isPublic() : true);
                clothing.setIsFavorite(false);

                Clothes saved = clothesRepository.save(clothing);
                clothingIds.add(saved.getId());
            }

            log.info("Saved {} advanced items", clothingIds.size());

            // Inicia processamento em batch
            log.info("Starting batch-advanced async processing (AI: {})...", body.processWithAI());
            processingService.processBatchClothingImagesAsync(clothingIds, body.processWithAI());

            List<String> clothingIdsAsStrings = clothingIds.stream()
                    .map(UUID::toString)
                    .toList();

            return ResponseEntity.ok(new BatchUploadResponseDTO(
                    clothingIdsAsStrings,
                    "Advanced batch upload successful - processing in background",
                    clothingIds.size()
            ));

        } catch (Exception e) {
            log.error("❌ Batch advanced upload error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDTO("Batch advanced upload failed", e.getMessage()));
        }
    }
}