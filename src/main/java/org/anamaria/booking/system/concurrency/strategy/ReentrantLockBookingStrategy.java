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
import java.util.concurrent.locks.ReentrantLock;

@Component
@RequiredArgsConstructor
public class ReentrantLockBookingStrategy implements BookingStrategy {

    private final BookingCore bookingCore;
    private final ConcurrentHashMap<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public StrategyName name() {
        return StrategyName.REENTRANT_LOCK;
    }

    @Override
    public Appointment book(CreateAppointmentRequest request, User client) {
        ReentrantLock lock = locks.computeIfAbsent(request.providerId(), id -> new ReentrantLock());
        lock.lock();
        try {
            return bookingCore.book(request, client);
        } finally {
            lock.unlock();
        }
    }
}
