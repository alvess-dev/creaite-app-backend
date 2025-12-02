// wardrobe-api/src/main/java/com/creaite/wardrobe_api/controllers/UserController.java
package com.creaite.wardrobe_api.controllers;

import com.creaite.wardrobe_api.domain.user.Clothes;
import com.creaite.wardrobe_api.domain.user.ClothingCategory;
import com.creaite.wardrobe_api.domain.user.User;
import com.creaite.wardrobe_api.dto.ClothesDTO;
import com.creaite.wardrobe_api.dto.UserDTO;
import com.creaite.wardrobe_api.repositories.ClothesRepository;
import com.creaite.wardrobe_api.repositories.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserRepository repository;
    private final ClothesRepository clothesRepository;

    @GetMapping
    public ResponseEntity<UserDTO> getUser(@AuthenticationPrincipal User userBody) {
        User user = repository.findByEmail(userBody.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(new UserDTO(
                user.getUsername(),
                user.getName(),
                user.getBirthDate(),
                user.getLanguage(),
                user.getProfilePictureUrl(),
                user.getBio(),
                user.getIsVerified(),
                user.getStatus(),
                user.getLastLogin(),
                user.getEmail(),
                user.getOauthProvider()
        ));
    }

    @PatchMapping("/update")
    public ResponseEntity<UserDTO> patchUser(@AuthenticationPrincipal User userBody, @RequestBody @Valid UserDTO body) {
        try {
            if (body.name() != null) userBody.setName(body.name());
            if (body.username() != null) userBody.setUsername(body.username());
            if (body.language() != null) userBody.setLanguage(body.language());
            if (body.profilePictureUrl() != null) userBody.setProfilePictureUrl(body.profilePictureUrl());
            if (body.bio() != null) userBody.setBio(body.bio());
            if (body.birthDate() != null) userBody.setBirthDate(body.birthDate());

            this.repository.save(userBody);

            return ResponseEntity.ok(new UserDTO(
                    userBody.getUsername(),
                    userBody.getName(),
                    userBody.getBirthDate(),
                    userBody.getLanguage(),
                    userBody.getProfilePictureUrl(),
                    userBody.getBio(),
                    userBody.getIsVerified(),
                    userBody.getStatus(),
                    userBody.getLastLogin(),
                    userBody.getEmail(),
                    userBody.getOauthProvider()
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/clothes")
    public ResponseEntity<List<ClothesDTO>> getClothes(@AuthenticationPrincipal User userBody, @RequestParam(required = false) String category) {
        try {
            User user = repository.findByEmail(userBody.getEmail()).orElseThrow(() -> new RuntimeException("User not found"));
            List<Clothes> clothesList;

            if (category != null) {
                ClothingCategory categoryEnum = ClothingCategory.valueOf(category.toUpperCase());
                clothesList = clothesRepository.findByUserIdAndCategory(user.getId(), categoryEnum);
            } else {
                clothesList = clothesRepository.findByUserId(user.getId());
            }

            // ✅ CORREÇÃO: Incluir os campos de processamento
            List<ClothesDTO> clothesDTOList = clothesList.stream()
                    .map(clothes -> new ClothesDTO(
                            clothes.getId(),
                            clothes.getName(),
                            clothes.getCategory(),
                            clothes.getColor(),
                            clothes.getBrand(),
                            clothes.getClothingPictureUrl(),
                            clothes.getOriginalImageUrl(), // ✅ ADICIONADO
                            clothes.getDescription(),
                            clothes.getIsPublic(),
                            clothes.getProcessingStatus(), // ✅ ADICIONADO
                            clothes.getProcessingError(),  // ✅ ADICIONADO
                            clothes.getCreatedAt(),        // ✅ ADICIONADO
                            clothes.getUpdatedAt()         // ✅ ADICIONADO
                    ))
                    .toList();

            return ResponseEntity.ok(clothesDTOList);

        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(@AuthenticationPrincipal User userBody) {
        try {
            User user = repository.findByEmail(userBody.getEmail()).orElseThrow(() -> new RuntimeException("User not found"));

            this.repository.delete(user);
            return ResponseEntity.ok("User deleted successfully");
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
}