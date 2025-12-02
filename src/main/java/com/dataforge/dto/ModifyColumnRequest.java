package com.dataforge.dto;

import jakarta.validation.constraints.NotBlank;

public record ModifyColumnRequest(
    String newColumnName, // Optional: if renaming the column
    String newDataType,   // Optional: if changing the column's data type
    Boolean isNullable,   // Optional: if changing nullability
    Boolean isUnique      // Optional: if changing uniqueness
) {
    // The isValid() method is no longer strictly necessary as validation will be handled by annotations
    // but can be kept for additional custom logic if needed.
    public boolean isValid() {
        // At least one modification must be specified
        return (newColumnName != null && !newColumnName.isBlank()) ||
               (newDataType != null && !newDataType.isBlank()) ||
               isNullable != null ||
               isUnique != null;
    }
}
