package org.anamaria.booking.system.concurrency;

import lombok.RequiredArgsConstructor;
import org.anamaria.booking.system.dto.CreateAppointmentRequest;
import org.anamaria.booking.system.model.Appointment;
import org.anamaria.booking.system.model.User;
import org.anamaria.booking.system.repository.AppointmentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class ConcurrentBookingRaceRunner {

    private final BookingStrategyRegistry strategyRegistry;
    private final BookingCore bookingCore;
    private final BookingMetrics bookingMetrics;
    private final AppointmentRepository appointmentRepository;

    public Map<String, Object> runSameSlotRace(
            StrategyName strategyName,
            CreateAppointmentRequest request,
            User client,
            int concurrency,
            boolean cleanupBeforeRun) {

        if (concurrency < 2) {
            throw new IllegalArgumentException("concurrency must be at least 2");
        }

        if (cleanupBeforeRun) {
            deleteBookedOverlapping(request.providerId(), request.startTime(), request.endTime());
        }

        bookingMetrics.reset();

        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        CountDownLatch ready = new CountDownLatch(concurrency);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        long wallStart = System.nanoTime();
        for (int i = 0; i < concurrency; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    bookingMetrics.timed(strategyName,
                            () -> strategyRegistry.book(strategyName, request, client));
                    successes.incrementAndGet();
                } catch (BookingConflictException ex) {
                    conflicts.incrementAndGet();
                } catch (Exception ex) {
                    errors.incrementAndGet();
                }
            }));
        }

        try {
            ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Race harness failed", ex);
        } finally {
            pool.shutdownNow();
        }

        long wallMs = (System.nanoTime() - wallStart) / 1_000_000L;
        int doubleBookings = bookingCore.countDoubleBookings(request.providerId());

        return Map.of(
                "strategy", strategyName.name(),
                "concurrency", concurrency,
                "successes", successes.get(),
                "conflicts", conflicts.get(),
                "errors", errors.get(),
                "doubleBookingPairs", doubleBookings,
                "wallClockMs", wallMs,
                "metrics", bookingMetrics.snapshot()
        );
    }

    private void deleteBookedOverlapping(Long providerId, LocalDateTime start, LocalDateTime end) {
        List<Appointment> overlapping = appointmentRepository.findOverlappingBooked(providerId, start, end);
        appointmentRepository.deleteAll(overlapping);
    }
}
