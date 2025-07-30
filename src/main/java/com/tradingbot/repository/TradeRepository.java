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

    /**
     * Finds all trades with a given status (e.g., "OPEN").
     */
    List<Trade> findByStatus(String status);

    /**
     *
     * Finds all trades for a specific strategy and status.
     */
    List<Trade> findByStrategyAndStatus(String strategy, String status);

    /**
     * Finds all trades, ordered by the most recent entry time first.
     * Useful for the dashboard display.
     */
    List<Trade> findAllByOrderByEntryTimestampDesc();

    /**
     * --- THIS IS THE FIX ---
     * Calculates the total realized Profit and Loss for trades that were CLOSED
     * within a specific time window (typically, for the current day).
     *
     * The query was updated to use `t.exitTimestamp` instead of the old `timestamp` field.
     * We also explicitly check for `status = 'CLOSED'` to ensure we only sum realized PnL.
     */
    @Query("SELECT sum(t.pnl) FROM Trade t WHERE t.status = 'CLOSED' AND t.exitTimestamp >= :start AND t.exitTimestamp < :end")
    BigDecimal findTodaysPnLBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}