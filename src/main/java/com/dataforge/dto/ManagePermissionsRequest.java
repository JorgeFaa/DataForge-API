package com.dataforge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ManagePermissionsRequest(
    @NotBlank(message = "Table name cannot be empty")
    String tableName,
    @NotNull(message = "Privileges list cannot be null")
    @NotEmpty(message = "At least one privilege must be specified")
    List<String> privileges // e.g., "SELECT", "INSERT", "UPDATE", "DELETE"
) {}
