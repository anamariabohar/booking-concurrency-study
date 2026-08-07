package org.anamaria.booking.system.controller;

import lombok.RequiredArgsConstructor;
import org.anamaria.booking.system.concurrency.BookingMetrics;
import org.anamaria.booking.system.concurrency.BookingStrategyRegistry;
import org.anamaria.booking.system.concurrency.StrategyName;
import org.anamaria.booking.system.dto.CreateAppointmentRequest;
import org.anamaria.booking.system.model.Appointment;
import org.anamaria.booking.system.model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments/book")
@RequiredArgsConstructor
public class ConcurrentBookingController {

    private final BookingStrategyRegistry strategyRegistry;
    private final BookingMetrics bookingMetrics;

    @PostMapping("/unsafe")
    public Appointment bookUnsafe(@RequestBody CreateAppointmentRequest request,
                                  @AuthenticationPrincipal User user) {
        return bookingMetrics.timed(StrategyName.UNSAFE,
                () -> strategyRegistry.book(StrategyName.UNSAFE, request, user));
    }

    @PostMapping("/synchronized")
    public Appointment bookSynchronized(@RequestBody CreateAppointmentRequest request,
                                        @AuthenticationPrincipal User user) {
        return bookingMetrics.timed(StrategyName.SYNCHRONIZED,
                () -> strategyRegistry.book(StrategyName.SYNCHRONIZED, request, user));
    }

    @PostMapping("/reentrant-lock")
    public Appointment bookReentrantLock(@RequestBody CreateAppointmentRequest request,
                                         @AuthenticationPrincipal User user) {
        return bookingMetrics.timed(StrategyName.REENTRANT_LOCK,
                () -> strategyRegistry.book(StrategyName.REENTRANT_LOCK, request, user));
    }

    @PostMapping("/pessimistic")
    public Appointment bookPessimistic(@RequestBody CreateAppointmentRequest request,
                                       @AuthenticationPrincipal User user) {
        return bookingMetrics.timed(StrategyName.PESSIMISTIC,
                () -> strategyRegistry.book(StrategyName.PESSIMISTIC, request, user));
    }

    @PostMapping("/optimistic")
    public Appointment bookOptimistic(@RequestBody CreateAppointmentRequest request,
                                      @AuthenticationPrincipal User user) {
        return bookingMetrics.timed(StrategyName.OPTIMISTIC,
                () -> strategyRegistry.book(StrategyName.OPTIMISTIC, request, user));
    }
}
