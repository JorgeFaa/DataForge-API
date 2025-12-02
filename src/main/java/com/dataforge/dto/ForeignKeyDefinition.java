package com.dataforge.dto;

import jakarta.validation.constraints.NotBlank;

public record ForeignKeyDefinition(
    @NotBlank(message = "Constraint name cannot be empty")
    String constraintName, // e.g., "fk_orders_customer"
    @NotBlank(message = "Local column cannot be empty")
    String localColumn,
    @NotBlank(message = "Foreign table cannot be empty")
    String foreignTable,
    @NotBlank(message = "Foreign column cannot be empty")
    String foreignColumn
) {
    public boolean isValid() {
        return constraintName != null && !constraintName.isBlank() &&
               localColumn != null && !localColumn.isBlank() &&
               foreignTable != null && !foreignTable.isBlank() &&
               foreignColumn != null && !foreignColumn.isBlank();
    }
}
