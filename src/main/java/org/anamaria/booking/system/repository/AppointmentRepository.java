package org.anamaria.booking.system.repository;

import org.anamaria.booking.system.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("""
            SELECT a FROM Appointment a
            WHERE a.provider.id = :providerId
              AND a.status = org.anamaria.booking.system.model.Appointment.Status.BOOKED
              AND a.startTime < :end
              AND a.endTime > :start
            """)
    List<Appointment> findOverlappingBooked(
            @Param("providerId") Long providerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
            SELECT a FROM Appointment a
            WHERE a.provider.id = :providerId
              AND a.status = org.anamaria.booking.system.model.Appointment.Status.BOOKED
            ORDER BY a.startTime
            """)
    List<Appointment> findBookedByProviderId(@Param("providerId") Long providerId);
}
