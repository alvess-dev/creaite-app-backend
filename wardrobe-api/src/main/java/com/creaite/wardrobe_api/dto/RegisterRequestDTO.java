package com.creaite.wardrobe_api.dto;

import java.time.LocalDate;

public record RegisterRequestDTO(
        String username,
        String email,
        String password,
        String language,
        String name,
        String birthDate  // String ao invés de LocalDate para facilitar parsing
) {}