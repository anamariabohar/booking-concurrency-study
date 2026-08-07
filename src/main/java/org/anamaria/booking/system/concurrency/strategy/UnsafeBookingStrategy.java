package org.anamaria.booking.system.concurrency.strategy;

import lombok.RequiredArgsConstructor;
import org.anamaria.booking.system.concurrency.BookingCore;
import org.anamaria.booking.system.concurrency.BookingStrategy;
import org.anamaria.booking.system.concurrency.StrategyName;
import org.anamaria.booking.system.dto.CreateAppointmentRequest;
import org.anamaria.booking.system.model.Appointment;
import org.anamaria.booking.system.model.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Intentionally racy check-then-act baseline for the concurrency study.
 */
@Component
@RequiredArgsConstructor
public class UnsafeBookingStrategy implements BookingStrategy {

    private final BookingCore bookingCore;

    @Override
    public StrategyName name() {
        return StrategyName.UNSAFE;
    }

    @Override
    @Transactional
    public Appointment book(CreateAppointmentRequest request, User client) {
        return bookingCore.book(request, client);
    }
}
