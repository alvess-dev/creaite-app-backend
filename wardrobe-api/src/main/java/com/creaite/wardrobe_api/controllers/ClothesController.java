package com.creaite.wardrobe_api.controllers;

import com.creaite.wardrobe_api.domain.user.Clothes;
import com.creaite.wardrobe_api.domain.user.User;
import com.creaite.wardrobe_api.dto.*;
import com.creaite.wardrobe_api.repositories.ClothesRepository;
import com.creaite.wardrobe_api.repositories.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/clothes")
@RequiredArgsConstructor
public class ClothesController {
    private final UserRepository repository;
    private final ClothesRepository clothesRepository;

    // ADICIONAR ROUPA NO GUARDA-ROUPA
    @PostMapping("/add")
    public ResponseEntity register(@AuthenticationPrincipal User userBody, @RequestBody @Valid ClothesDTO body) {
        try {
            User user = repository.findByEmail(userBody.getEmail()).orElseThrow(() -> new RuntimeException("User not found"));
            Clothes newClothing = new Clothes();

            newClothing.setUserId(user.getId());
            newClothing.setName(body.name());
            newClothing.setCategory(body.category());
            newClothing.setColor(body.color());
            newClothing.setBrand(body.brand());
            newClothing.setClothingPictureUrl(body.clothingPictureUrl());
            newClothing.setDescription(body.description());
            newClothing.setIsPublic(body.isPublic());
            clothesRepository.save(newClothing);

            return ResponseEntity.ok(new ClothesDTO(
                    body.name(),
                    body.category(),
                    body.color(),
                    body.brand(),
                    body.clothingPictureUrl(),
                    body.description(),
                    body.isPublic()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

