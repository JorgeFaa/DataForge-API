package com.dataforge.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL) // Don't include null fields in the JSON response
public record SqlQueryResponse(
    boolean success,
    String message,
    Boolean isQueryResult, // True if it was a SELECT, false otherwise
    List<String> columns,  // Column names for a SELECT query
    List<Map<String, Object>> rows, // Data rows for a SELECT query
    Integer rowsAffected   // Number of rows affected by an UPDATE, INSERT, or DELETE
) {
    // Factory method for successful SELECT queries
    public static SqlQueryResponse forSelect(List<String> columns, List<Map<String, Object>> rows) {
        return new SqlQueryResponse(true, "Query executed successfully.", true, columns, rows, null);
    }

    // Factory method for successful DML (Data Manipulation Language) queries
    public static SqlQueryResponse forUpdate(int rowsAffected) {
        return new SqlQueryResponse(true, "Update executed successfully.", false, null, null, rowsAffected);
    }

    // Factory method for errors
    public static SqlQueryResponse forError(String message) {
        return new SqlQueryResponse(false, message, null, null, null, null);
    }
}
