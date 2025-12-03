package com.dataforge.dto;

import jakarta.validation.constraints.NotBlank;

public record SqlQueryRequest(
    @NotBlank(message = "SQL query cannot be empty")
    String sql,
    
    // The master password is required to execute a direct query
    @NotBlank(message = "Master password is required for this operation")
    String masterPassword
) {}
