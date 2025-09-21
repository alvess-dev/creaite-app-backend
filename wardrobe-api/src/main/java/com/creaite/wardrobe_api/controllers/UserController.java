package com.creaite.wardrobe_api.controllers;

import com.creaite.wardrobe_api.domain.user.User;
import com.creaite.wardrobe_api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}