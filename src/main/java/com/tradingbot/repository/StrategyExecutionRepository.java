package com.tradingbot.repository;

import com.tradingbot.entity.StrategyExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface StrategyExecutionRepository extends JpaRepository<StrategyExecution, Long> {
    Optional<StrategyExecution> findByStatus(String status);
    Optional<StrategyExecution> findByStrategyId(String strategyId);
}
