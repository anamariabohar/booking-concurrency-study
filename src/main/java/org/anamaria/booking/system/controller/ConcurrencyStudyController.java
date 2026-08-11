package org.anamaria.booking.system.controller;

import lombok.RequiredArgsConstructor;
import org.anamaria.booking.system.concurrency.BookingCore;
import org.anamaria.booking.system.concurrency.BookingMetrics;
import org.anamaria.booking.system.concurrency.ConcurrentBookingRaceRunner;
import org.anamaria.booking.system.concurrency.StrategyName;
import org.anamaria.booking.system.dto.CreateAppointmentRequest;
import org.anamaria.booking.system.model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/concurrency")
@RequiredArgsConstructor
public class ConcurrencyStudyController {

    private final BookingMetrics bookingMetrics;
    private final BookingCore bookingCore;
    private final ConcurrentBookingRaceRunner raceRunner;

    @GetMapping("/metrics")
    public Map<String, Object> metrics() {
        return bookingMetrics.snapshot();
    }

    @PostMapping("/metrics/reset")
    public Map<String, String> resetMetrics() {
        bookingMetrics.reset();
        return Map.of("status", "reset");
    }

    @GetMapping("/double-bookings")
    public Map<String, Object> doubleBookings(@RequestParam("providerId") Long providerId) {
        return Map.of(
                "providerId", providerId,
                "doubleBookingPairs", bookingCore.countDoubleBookings(providerId),
                "overlappingAppointments", bookingCore.findOverlaps(providerId)
        );
    }

    /**
     * In-process same-slot race for correctness experiments (no HTTP fan-out needed).
     * strategy: UNSAFE | SYNCHRONIZED | REENTRANT_LOCK | PESSIMISTIC | OPTIMISTIC
     */
    @PostMapping("/race")
    public Map<String, Object> race(
            @RequestParam("strategy") StrategyName strategy,
            @RequestParam(value = "concurrency", defaultValue = "50") int concurrency,
            @RequestParam(value = "cleanupBeforeRun", defaultValue = "true") boolean cleanupBeforeRun,
            @RequestBody CreateAppointmentRequest request,
            @AuthenticationPrincipal User user) {
        return raceRunner.runSameSlotRace(strategy, request, user, concurrency, cleanupBeforeRun);
    }
}
