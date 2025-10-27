package com.tradingbot.repository;

import com.tradingbot.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

/**
 * Repository interface for managing Trade entities.
 * Provides methods for CRUD operations and custom queries.
 */
@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

    /**
     * Finds all trades with a given status (e.g., "OPEN").
     *
     * @param status the status to filter trades by (must not be null)
     * @return a list of trades with the specified status
     */
    @NonNull
    List<Trade> findByStatus(@NonNull Trade.TradeStatus status);

    /**
     * Find a trade by the external order id.
     */
    Optional<Trade> findByOrderId(String orderId);

    /**
     * Finds all trades for a specific strategy and status.
     *
     * @param strategy the strategy to filter trades by (must not be null)
     * @param status   the status to filter trades by (must not be null)
     * @return a list of trades matching the specified strategy and status
     */
    @NonNull
    List<Trade> findByStrategyAndStatus(@NonNull String strategy, @NonNull String status);

    /**
     * Finds all trades, ordered by the most recent entry time first.
     * Useful for the dashboard display.
     *
     * @return a list of trades ordered by entry timestamp in descending order
     */
    @NonNull
    List<Trade> findAllByOrderByEntryTimestampDesc();

    /**
     * Calculates the total realized Profit and Loss for trades that were CLOSED
     * within a specific time window (typically, for the current day).
     *
     * @param start the start of the time window (must not be null)
     * @param end   the end of the time window (must not be null)
     * @return the total realized PnL for the specified time window
     */
    @Query("SELECT SUM(t.pnl) FROM Trade t WHERE t.status = 'CLOSED' AND t.exitTimestamp >= :start AND t.exitTimestamp < :end")
    @NonNull
    BigDecimal findTodaysPnLBetween(@Param("start") @NonNull LocalDateTime start, @Param("end") @NonNull LocalDateTime end);
}