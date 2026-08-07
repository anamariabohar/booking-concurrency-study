package org.anamaria.booking.system.dto;

public record ProviderRegisterRequest(
        String username,
        String email,
        String password,
        String specialization,
        Integer avgAppointmentDuration
) {}
