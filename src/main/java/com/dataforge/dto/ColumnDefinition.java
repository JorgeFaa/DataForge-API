package com.dataforge.dto;

import jakarta.validation.constraints.NotBlank;

public record ColumnDefinition(
    @NotBlank(message = "Column name cannot be empty")
    String name,
    @NotBlank(message = "Data type cannot be empty")
    String dataType, // e.g., "VARCHAR(255)", "INTEGER", "BOOLEAN"
    boolean isPrimaryKey,
    boolean isNullable,
    boolean isUnique
) {
    // Records are immutable and provide constructor, getters, equals, hashCode, and toString
}
