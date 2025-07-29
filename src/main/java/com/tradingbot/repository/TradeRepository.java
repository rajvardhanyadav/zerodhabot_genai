package com.tradingbot.repository;

import com.tradingbot.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findByStatus(String status);

    /**
     * Calculates the sum of PnL for trades within a given time window.
     * Using parameters for the date range is more portable across different databases
     * than using database-specific functions like DATE().
     *
     * @param startOfDay The start of the day (e.g., today at 00:00:00).
     * @param endOfDay   The end of the day (e.g., today at 23:59:59.999).
     * @return The total PnL as a BigDecimal, or 0 if no trades are found.
     */
    @Query("SELECT COALESCE(SUM(t.pnl), 0) FROM Trade t WHERE t.timestamp >= :startOfDay AND t.timestamp < :endOfDay")
    BigDecimal findTodaysPnLBetween(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    List<Trade> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<Trade> findByStrategyAndStatus(String strategy, String status);
}
