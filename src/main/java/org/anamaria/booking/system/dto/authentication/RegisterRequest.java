package org.anamaria.booking.system.dto.authentication;

public record RegisterRequest(
        String username,
        String email,
        String password,
        String role
) {}
