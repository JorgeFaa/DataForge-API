package com.dataforge.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDbUserRequest(
    @NotBlank(message = "Username cannot be empty")
    String username,
    @NotBlank(message = "Password cannot be empty")
    String password
) {}
