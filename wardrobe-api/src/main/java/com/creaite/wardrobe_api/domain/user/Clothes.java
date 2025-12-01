// wardrobe-api/src/main/java/com/creaite/wardrobe_api/domain/user/Clothes.java
package com.creaite.wardrobe_api.domain.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "clothing_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Clothes {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50)
    private ClothingCategory category;

    @Column(name = "color")
    private String color;

    @Column(name = "brand")
    private String brand;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String clothingPictureUrl;

    @Column(name = "original_image_url", columnDefinition = "TEXT")
    private String originalImageUrl;

    @Column(name = "description")
    private String description;

    @Column(name = "is_public")
    private Boolean isPublic = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false)
    private ProcessingStatus processingStatus = ProcessingStatus.COMPLETED;

    @Column(name = "processing_error")
    private String processingError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ProcessingStatus {
        PENDING,      // Aguardando processamento
        PROCESSING,   // Sendo processada pela IA
        COMPLETED,    // Processamento concluído
        FAILED        // Falha no processamento
    }
}