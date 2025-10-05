package com.creaite.wardrobe_api.repositories;

import com.creaite.wardrobe_api.domain.user.Clothes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClothesRepository extends JpaRepository<Clothes, UUID> {
    List<Clothes> findByUserId(UUID userId);
}