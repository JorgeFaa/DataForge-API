package com.dataforge.dto;

public record TableRelationship(
    String constraintName,
    String localTableName, // The table where the foreign key is defined
    String localColumnName,
    String foreignTableName, // The table being referenced
    String foreignColumnName
) {}
