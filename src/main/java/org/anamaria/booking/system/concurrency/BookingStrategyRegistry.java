package org.anamaria.booking.system.concurrency;

import org.anamaria.booking.system.dto.CreateAppointmentRequest;
import org.anamaria.booking.system.model.Appointment;
import org.anamaria.booking.system.model.User;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class BookingStrategyRegistry {

    private final Map<StrategyName, BookingStrategy> strategies = new EnumMap<>(StrategyName.class);

    public BookingStrategyRegistry(List<BookingStrategy> strategyList) {
        for (BookingStrategy strategy : strategyList) {
            strategies.put(strategy.name(), strategy);
        }
    }

    public BookingStrategy get(StrategyName name) {
        BookingStrategy strategy = strategies.get(name);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown booking strategy: " + name);
        }
        return strategy;
    }

    public Appointment book(StrategyName name, CreateAppointmentRequest request, User client) {
        return get(name).book(request, client);
    }
}
