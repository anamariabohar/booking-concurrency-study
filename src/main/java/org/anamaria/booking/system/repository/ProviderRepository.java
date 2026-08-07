package org.anamaria.booking.system.repository;

import jakarta.persistence.LockModeType;
import org.anamaria.booking.system.model.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProviderRepository extends JpaRepository<Provider, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Provider p WHERE p.id = :id")
    Optional<Provider> findByIdForUpdate(@Param("id") Long id);
}
