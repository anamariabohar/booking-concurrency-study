package org.anamaria.booking.system.concurrency.strategy;

import lombok.RequiredArgsConstructor;
import org.anamaria.booking.system.concurrency.BookingConflictException;
import org.anamaria.booking.system.concurrency.BookingCore;
import org.anamaria.booking.system.concurrency.BookingStrategy;
import org.anamaria.booking.system.concurrency.StrategyName;
import org.anamaria.booking.system.dto.CreateAppointmentRequest;
import org.anamaria.booking.system.model.Appointment;
import org.anamaria.booking.system.model.Provider;
import org.anamaria.booking.system.model.User;
import org.anamaria.booking.system.repository.ProviderRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OptimisticBookingStrategy implements BookingStrategy {

    private final BookingCore bookingCore;
    private final ProviderRepository providerRepository;

    @Override
    public StrategyName name() {
        return StrategyName.OPTIMISTIC;
    }

    @Override
    @Transactional
    public Appointment book(CreateAppointmentRequest request, User client) {
        try {
            Provider provider = providerRepository.findById(request.providerId())
                    .orElseThrow(() -> new IllegalArgumentException("Provider not found"));

            // Claim the provider version before inserting so losers fail before double-booking.
            provider.setSpecialization(provider.getSpecialization());
            providerRepository.saveAndFlush(provider);

            return bookingCore.bookWithProvider(provider, request, client);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new BookingConflictException(
                    "Optimistic lock conflict while booking appointment", ex);
        }
    }
}
