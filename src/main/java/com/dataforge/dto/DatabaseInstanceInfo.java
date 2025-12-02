package com.dataforge.dto;

public record DatabaseInstanceInfo(
    Long id,
    String dbName,
    String user,
    String host,
    int port
) {}
