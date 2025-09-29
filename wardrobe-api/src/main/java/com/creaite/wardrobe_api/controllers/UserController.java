package com.creaite.wardrobe_api.controllers;

import com.creaite.wardrobe_api.domain.user.User;
import com.creaite.wardrobe_api.dto.RegisterRequestDTO;
import com.creaite.wardrobe_api.dto.ResponseDTO;
import com.creaite.wardrobe_api.dto.UserRequestDTO;
import com.creaite.wardrobe_api.dto.UserResponseDTO;
import com.creaite.wardrobe_api.repositories.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserRepository repository;
    @GetMapping
    public ResponseEntity<String> getUser(@AuthenticationPrincipal User userBody) {
        User user = this.repository.findByEmail(userBody.getEmail()).orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok("Olá " + user.getName());
    }
    @PutMapping("/update")
    public ResponseEntity register(@AuthenticationPrincipal User userBody, @RequestBody @Valid UserRequestDTO body){
        try{
            userBody.setName(body.name());
            userBody.setBio(body.bio());

            this.repository.save(userBody);

            return ResponseEntity.ok(new UserResponseDTO(userBody.getName(), userBody.getBio()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}