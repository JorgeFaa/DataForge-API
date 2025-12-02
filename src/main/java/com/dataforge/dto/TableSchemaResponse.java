package com.dataforge.dto;

import java.util.List;

/**
 * Represents the schema of a database table.
 *
 * @param tableName The name of the table.
 * @param columns   A list of column definitions for the table.
 */
public record TableSchemaResponse(
    String tableName,
    List<ColumnDefinition> columns
) {}
