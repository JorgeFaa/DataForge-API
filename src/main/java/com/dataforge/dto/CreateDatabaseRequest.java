package com.dataforge.dto;

import jakarta.validation.constraints.NotBlank; // Import NotBlank

// Using record for an immutable, concise DTO
public record CreateDatabaseRequest(
    @NotBlank(message = "Database name cannot be empty")
    String dbName,
    @NotBlank(message = "User cannot be empty")
    String user,
    @NotBlank(message = "Password cannot be empty")
    String password
) {
    // The isValid() method is no longer strictly necessary as validation will be handled by annotations
    // but can be kept for additional custom logic if needed.
    public boolean isValid() {
        return dbName != null && !dbName.isBlank() &&
               user != null && !user.isBlank() &&
               password != null && !password.isBlank();
    }
}
