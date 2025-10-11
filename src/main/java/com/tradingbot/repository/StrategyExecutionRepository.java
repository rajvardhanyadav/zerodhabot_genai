package com.tradingbot.repository;

import com.tradingbot.entity.StrategyExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for managing StrategyExecution entities.
 * Provides methods for CRUD operations and custom queries.
 */
@Repository
public interface StrategyExecutionRepository extends JpaRepository<StrategyExecution, Long> {

    /**
     * Finds a StrategyExecution entity by its status.
     *
     * @param status the status to search for (must not be null)
     * @return an Optional containing the StrategyExecution entity if found, or empty if not found
     */
    @NonNull
    Optional<StrategyExecution> findByStatus(@NonNull String status);

    /**
     * Finds a StrategyExecution entity by its strategy ID.
     *
     * @param strategyId the strategy ID to search for (must not be null)
     * @return an Optional containing the StrategyExecution entity if found, or empty if not found
     */
    @NonNull
    Optional<StrategyExecution> findByStrategyId(@NonNull String strategyId);
}