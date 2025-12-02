package com.dataforge.dto;

public record DbUserInfo(
    String username,
    boolean canCreateDb,
    boolean canCreateRole
) {}
