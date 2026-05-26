package com.innowise.userservice.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentCardResponseDTO(
        Long id,
        String number,
        String holder,
        LocalDate expirationDate,
        boolean active,
        Long userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
