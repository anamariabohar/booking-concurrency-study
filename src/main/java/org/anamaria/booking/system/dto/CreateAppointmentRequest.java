package org.anamaria.booking.system.dto;

import java.time.LocalDateTime;

public record CreateAppointmentRequest(
        Long providerId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String type
) {}