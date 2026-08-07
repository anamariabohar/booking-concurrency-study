package org.anamaria.booking.system.concurrency.strategy;

import lombok.RequiredArgsConstructor;
import org.anamaria.booking.system.concurrency.BookingCore;
import org.anamaria.booking.system.concurrency.BookingStrategy;
import org.anamaria.booking.system.concurrency.StrategyName;
import org.anamaria.booking.system.dto.CreateAppointmentRequest;
import org.anamaria.booking.system.model.Appointment;
import org.anamaria.booking.system.model.Provider;
import org.anamaria.booking.system.model.User;
import org.anamaria.booking.system.repository.ProviderRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PessimisticBookingStrategy implements BookingStrategy {

    private final BookingCore bookingCore;
    private final ProviderRepository providerRepository;

    @Override
    public StrategyName name() {
        return StrategyName.PESSIMISTIC;
    }

    @Override
    @Transactional
    public Appointment book(CreateAppointmentRequest request, User client) {
        Provider provider = providerRepository.findByIdForUpdate(request.providerId())
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));
        return bookingCore.bookWithProvider(provider, request, client);
    }
}
