package com.innowise.userservice.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserResponseDTO(
        Long id,
        String name,
        String surname,
        LocalDate birthDate,
        String email,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
