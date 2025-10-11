package com.tradingbot.repository;

import com.tradingbot.entity.DailyPnL;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Repository interface for managing DailyPnL entities.
 * Provides methods for CRUD operations and custom queries.
 */
@Repository
public interface DailyPnLRepository extends JpaRepository<DailyPnL, Long> {

    /**
     * Finds a DailyPnL entity by its date.
     *
     * @param date the date to search for (must not be null)
     * @return an Optional containing the DailyPnL entity if found, or empty if not found
     */
    @NonNull
    Optional<DailyPnL> findByDate(@NonNull LocalDate date);
}