package org.anamaria.booking.system.repository;

import org.anamaria.booking.system.model.WorkingHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkingHoursRepository extends JpaRepository<WorkingHours, Long> {

    List<WorkingHours> findByProviderIdAndDayOfWeek(Long providerId, Integer dayOfWeek);
}