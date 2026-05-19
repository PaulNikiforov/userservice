package com.innowise.userservice.model.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PaymentCardRequestDTO(
        @NotBlank(message = "Card number is required")
        @Pattern(regexp = "\\d{16}", message = "Card number must be 16 digits")
        String number,

        @NotBlank(message = "Card holder is required")
        @Size(min = 2, max = 100, message = "Holder name must be between 2 and 100 characters")
        String holder,

        @Future(message = "Expiration date must be in the future")
        LocalDate expirationDate
) {}
