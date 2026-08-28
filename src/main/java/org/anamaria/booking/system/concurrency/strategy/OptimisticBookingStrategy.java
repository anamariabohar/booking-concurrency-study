package org.anamaria.booking.system.concurrency.strategy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
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
import org.hibernate.StaleObjectStateException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OptimisticBookingStrategy implements BookingStrategy {

    private final BookingCore bookingCore;
    private final ProviderRepository providerRepository;
    private final EntityManager entityManager;
    private final OptimisticBookingStrategy self;

    public OptimisticBookingStrategy(BookingCore bookingCore,
                                     ProviderRepository providerRepository,
                                     EntityManager entityManager,
                                     @Lazy OptimisticBookingStrategy self) {
        this.bookingCore = bookingCore;
        this.providerRepository = providerRepository;
        this.entityManager = entityManager;
        this.self = self;
    }

    @Override
    public StrategyName name() {
        return StrategyName.OPTIMISTIC;
    }

    /**
     * Catch wraps the transactional proxy call so commit-time version conflicts
     * (UnexpectedRollbackException wrapping the optimistic lock failure) are
     * converted to BookingConflictException → HTTP 409.
     */
    @Override
    public Appointment book(CreateAppointmentRequest request, User client) {
        try {
            return self.doBook(request, client);
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException
                 | StaleObjectStateException | UnexpectedRollbackException ex) {
            throw new BookingConflictException(
                    "Optimistic lock conflict while booking appointment", ex);
        }
    }

    @Transactional
    public Appointment doBook(CreateAppointmentRequest request, User client) {
        Provider provider = providerRepository.findById(request.providerId())
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));

        entityManager.lock(provider, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

        return bookingCore.bookWithProvider(provider, request, client);
    }
}
