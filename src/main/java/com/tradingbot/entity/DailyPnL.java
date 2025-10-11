package com.tradingbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity representing the daily profit and loss (PnL) for trading.
 */
@Entity
@Table(name = "daily_pnl")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public final class DailyPnL {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The date for the daily PnL record.
     */
    private LocalDate date;

    /**
     * The total profit or loss for the day.
     */
    @Builder.Default
    private BigDecimal totalPnl = BigDecimal.ZERO;

    /**
     * The total number of trades executed on the day.
     */
    @Builder.Default
    private Integer totalTrades = 0;

    /**
     * Indicates whether trading was stopped for the day.
     */
    @Builder.Default
    private Boolean tradingStopped = false;

    public DailyPnL(LocalDate date) {
        this.date = date;
        this.totalPnl = BigDecimal.ZERO; // Default value
        this.totalTrades = 0; // Default value
        this.tradingStopped = false; // Default value
    }
}