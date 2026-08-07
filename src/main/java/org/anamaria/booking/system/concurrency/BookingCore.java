package org.anamaria.booking.system.concurrency;

import lombok.RequiredArgsConstructor;
import org.anamaria.booking.system.dto.CreateAppointmentRequest;
import org.anamaria.booking.system.model.Appointment;
import org.anamaria.booking.system.model.Provider;
import org.anamaria.booking.system.model.User;
import org.anamaria.booking.system.model.WorkingHours;
import org.anamaria.booking.system.repository.AppointmentRepository;
import org.anamaria.booking.system.repository.ProviderRepository;
import org.anamaria.booking.system.repository.WorkingHoursRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared booking validation and persistence without any concurrency control.
 * Strategies wrap this core with different locking / threading approaches.
 */
@Service
@RequiredArgsConstructor
public class BookingCore {

    private final AppointmentRepository appointmentRepository;
    private final ProviderRepository providerRepository;
    private final WorkingHoursRepository workingHoursRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public Appointment book(CreateAppointmentRequest request, User client) {
        Provider provider = providerRepository.findById(request.providerId())
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));
        return bookWithProvider(provider, request, client);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Appointment bookWithProvider(Provider provider, CreateAppointmentRequest request, User client) {
        if (client.getRole() != User.Role.CLIENT) {
            throw new IllegalArgumentException("Only clients can book appointments");
        }

        LocalDateTime start = request.startTime();
        LocalDateTime end = request.endTime();

        if (end == null || !end.isAfter(start)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }

        validateWorkingHours(provider, start, end);
        validateNoOverlap(provider.getId(), start, end);

        Appointment appointment = Appointment.builder()
                .provider(provider)
                .client(client)
                .startTime(start)
                .endTime(end)
                .type(request.type())
                .status(Appointment.Status.BOOKED)
                .build();

        return appointmentRepository.save(appointment);
    }

    private void validateNoOverlap(Long providerId, LocalDateTime start, LocalDateTime end) {
        List<Appointment> overlapping = appointmentRepository.findOverlappingBooked(providerId, start, end);
        if (!overlapping.isEmpty()) {
            throw new BookingConflictException("Appointment overlaps with an existing booking");
        }
    }

    public int countDoubleBookings(Long providerId) {
        List<Appointment> booked = appointmentRepository.findBookedByProviderId(providerId);
        int conflicts = 0;
        for (int i = 0; i < booked.size(); i++) {
            for (int j = i + 1; j < booked.size(); j++) {
                Appointment a = booked.get(i);
                Appointment b = booked.get(j);
                if (a.getStartTime().isBefore(b.getEndTime()) && b.getStartTime().isBefore(a.getEndTime())) {
                    conflicts++;
                }
            }
        }
        return conflicts;
    }

    public List<Appointment> findOverlaps(Long providerId) {
        List<Appointment> booked = appointmentRepository.findBookedByProviderId(providerId);
        List<Appointment> involved = new ArrayList<>();
        for (int i = 0; i < booked.size(); i++) {
            for (int j = i + 1; j < booked.size(); j++) {
                Appointment a = booked.get(i);
                Appointment b = booked.get(j);
                if (a.getStartTime().isBefore(b.getEndTime()) && b.getStartTime().isBefore(a.getEndTime())) {
                    if (!involved.contains(a)) {
                        involved.add(a);
                    }
                    if (!involved.contains(b)) {
                        involved.add(b);
                    }
                }
            }
        }
        return involved;
    }

    private void validateWorkingHours(Provider provider, LocalDateTime start, LocalDateTime end) {
        int dayOfWeek = start.getDayOfWeek().getValue();
        List<WorkingHours> hours = workingHoursRepository
                .findByProviderIdAndDayOfWeek(provider.getId(), dayOfWeek);

        if (hours.isEmpty()) {
            throw new IllegalArgumentException("Provider does not work on this day");
        }

        boolean insideWorkingHours = hours.stream().anyMatch(wh ->
                !start.toLocalTime().isBefore(wh.getStartTime())
                        && !end.toLocalTime().isAfter(wh.getEndTime())
        );

        if (!insideWorkingHours) {
            throw new IllegalArgumentException("Appointment outside provider working hours");
        }
    }
}
