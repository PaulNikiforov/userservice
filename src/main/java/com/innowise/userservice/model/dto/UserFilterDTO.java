package com.innowise.userservice.model.dto;

public record UserFilterDTO(
        String name,
        String surname,
        String email,
        Boolean active
) {}
