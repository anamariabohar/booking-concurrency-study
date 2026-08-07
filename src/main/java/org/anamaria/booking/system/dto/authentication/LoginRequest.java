package org.anamaria.booking.system.dto.authentication;

public record LoginRequest(
        String username,
        String password
) {}