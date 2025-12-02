package com.dataforge.dto;

import java.util.List;

public record UserPermissionInfo(
    String tableName,
    List<String> privileges
) {}
