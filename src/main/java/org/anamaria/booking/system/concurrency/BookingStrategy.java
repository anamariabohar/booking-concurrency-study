package org.anamaria.booking.system.concurrency;

import org.anamaria.booking.system.dto.CreateAppointmentRequest;
import org.anamaria.booking.system.model.Appointment;
import org.anamaria.booking.system.model.User;

public interface BookingStrategy {

    StrategyName name();

    Appointment book(CreateAppointmentRequest request, User client);
}
