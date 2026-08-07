package org.anamaria.booking.system.concurrency.threading;

import org.anamaria.booking.system.concurrency.BookingMetrics;
import org.anamaria.booking.system.concurrency.StrategyName;
import org.anamaria.booking.system.concurrency.strategy.ReentrantLockBookingStrategy;
import org.anamaria.booking.system.dto.CreateAppointmentRequest;
import org.anamaria.booking.system.model.Appointment;
import org.anamaria.booking.system.model.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Threading-model variants. All use {@link ReentrantLockBookingStrategy} as the correct
 * booking core so the measured variable is the threading model, not locking correctness.
 */
@Service
public class ThreadedBookingService {

    private final ReentrantLockBookingStrategy reentrantLockBookingStrategy;
    private final BookingMetrics bookingMetrics;
    private final ExecutorService bookingExecutor;
    private final ExecutorService virtualThreadBookingExecutor;

    public ThreadedBookingService(
            ReentrantLockBookingStrategy reentrantLockBookingStrategy,
            BookingMetrics bookingMetrics,
            @Qualifier("bookingExecutor") ExecutorService bookingExecutor,
            @Qualifier("virtualThreadBookingExecutor") ExecutorService virtualThreadBookingExecutor) {
        this.reentrantLockBookingStrategy = reentrantLockBookingStrategy;
        this.bookingMetrics = bookingMetrics;
        this.bookingExecutor = bookingExecutor;
        this.virtualThreadBookingExecutor = virtualThreadBookingExecutor;
    }

    public Appointment bookBlocking(CreateAppointmentRequest request, User client) {
        return bookingMetrics.timed(StrategyName.BLOCKING,
                () -> reentrantLockBookingStrategy.book(request, client));
    }

    public CompletableFuture<Appointment> bookOnExecutor(CreateAppointmentRequest request, User client) {
        return CompletableFuture.supplyAsync(
                () -> bookingMetrics.timed(StrategyName.EXECUTOR,
                        () -> reentrantLockBookingStrategy.book(request, client)),
                bookingExecutor
        );
    }

    public CompletableFuture<Appointment> bookCompletableFuture(CreateAppointmentRequest request, User client) {
        return CompletableFuture
                .supplyAsync(() -> request, bookingExecutor)
                .thenApplyAsync(req -> bookingMetrics.timed(StrategyName.COMPLETABLE_FUTURE,
                        () -> reentrantLockBookingStrategy.book(req, client)), bookingExecutor);
    }

    public CompletableFuture<Appointment> bookOnVirtualThread(CreateAppointmentRequest request, User client) {
        return CompletableFuture.supplyAsync(
                () -> bookingMetrics.timed(StrategyName.VIRTUAL_THREAD,
                        () -> reentrantLockBookingStrategy.book(request, client)),
                virtualThreadBookingExecutor
        );
    }
}
