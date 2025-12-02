package com.dataforge.dto;

import jakarta.validation.Valid; // Import @Valid
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty; // Import @NotEmpty
import jakarta.validation.constraints.NotNull; // Import @NotNull

import java.util.List;

public record CreateTableRequest(
    @NotBlank(message = "Table name cannot be empty")
    String tableName,
    @NotNull(message = "Columns list cannot be null")
    @NotEmpty(message = "Table must have at least one column")
    List<@Valid ColumnDefinition> columns, // @Valid to cascade validation to ColumnDefinition
    List<@Valid ForeignKeyDefinition> foreignKeys // @Valid to cascade validation to ForeignKeyDefinition
) {
    // The isValid() method is no longer strictly necessary as validation will be handled by annotations
    // but can be kept for additional custom logic if needed.
    public boolean isValid() {
        return tableName != null && !tableName.isBlank() &&
               columns != null && !columns.isEmpty();
    }
}
