package org.anamaria.booking.system.concurrency.strategy;

import lombok.RequiredArgsConstructor;
import org.anamaria.booking.system.concurrency.BookingCore;
import org.anamaria.booking.system.concurrency.BookingStrategy;
import org.anamaria.booking.system.concurrency.StrategyName;
import org.anamaria.booking.system.dto.CreateAppointmentRequest;
import org.anamaria.booking.system.model.Appointment;
import org.anamaria.booking.system.model.User;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class SynchronizedBookingStrategy implements BookingStrategy {

    private final BookingCore bookingCore;
    private final ConcurrentHashMap<Long, Object> locks = new ConcurrentHashMap<>();

    @Override
    public StrategyName name() {
        return StrategyName.SYNCHRONIZED;
    }

    @Override
    public Appointment book(CreateAppointmentRequest request, User client) {
        Object lock = locks.computeIfAbsent(request.providerId(), id -> new Object());
        synchronized (lock) {
            return bookingCore.book(request, client);
        }
    }
}
