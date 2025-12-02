package com.dataforge.dto;

// Using record for an immutable, concise DTO
public record CreateDatabaseResponse(
    String containerId,
    String dbName,
    String user,
    String host,
    int port
) {}
