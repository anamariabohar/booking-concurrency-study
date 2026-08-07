package org.anamaria.booking.system.controller;

import lombok.RequiredArgsConstructor;
import org.anamaria.booking.system.concurrency.threading.ThreadedBookingService;
import org.anamaria.booking.system.dto.CreateAppointmentRequest;
import org.anamaria.booking.system.model.Appointment;
import org.anamaria.booking.system.model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/appointments/book")
@RequiredArgsConstructor
public class ThreadedBookingController {

    private final ThreadedBookingService threadedBookingService;

    @PostMapping("/blocking")
    public Appointment bookBlocking(@RequestBody CreateAppointmentRequest request,
                                    @AuthenticationPrincipal User user) {
        return threadedBookingService.bookBlocking(request, user);
    }

    @PostMapping("/executor")
    public CompletableFuture<Appointment> bookExecutor(@RequestBody CreateAppointmentRequest request,
                                                       @AuthenticationPrincipal User user) {
        return threadedBookingService.bookOnExecutor(request, user);
    }

    @PostMapping("/completable-future")
    public CompletableFuture<Appointment> bookCompletableFuture(@RequestBody CreateAppointmentRequest request,
                                                                @AuthenticationPrincipal User user) {
        return threadedBookingService.bookCompletableFuture(request, user);
    }

    @PostMapping("/virtual-thread")
    public CompletableFuture<Appointment> bookVirtualThread(@RequestBody CreateAppointmentRequest request,
                                                            @AuthenticationPrincipal User user) {
        return threadedBookingService.bookOnVirtualThread(request, user);
    }
}
