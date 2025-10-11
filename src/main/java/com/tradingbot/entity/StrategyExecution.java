package com.tradingbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing the execution details of a trading strategy.
 */
@Entity
@Table(name = "strategy_executions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public final class StrategyExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique identifier for the strategy. */
    @Column(name = "strategy_id", unique = true)
    private String strategyId;

    /** The time when the strategy was entered. */
    @Column(name = "entry_time")
    private LocalDateTime entryTime;

    /** The time when the strategy was exited. */
    @Column(name = "exit_time")
    private LocalDateTime exitTime;

    /** The ATM strike price at the time of entry. */
    @Column(name = "atm_strike")
    private double atmStrike;

    /** The call option symbol used in the strategy. */
    @Column(name = "ce_symbol")
    private String ceSymbol;

    /** The put option symbol used in the strategy. */
    @Column(name = "pe_symbol")
    private String peSymbol;

    /** The entry price of the call option. */
    @Column(name = "ce_entry_price")
    private double ceEntryPrice;

    /** The entry price of the put option. */
    @Column(name = "pe_entry_price")
    private double peEntryPrice;

    /** The exit price of the call option. */
    @Column(name = "ce_exit_price")
    private double ceExitPrice;

    /** The exit price of the put option. */
    @Column(name = "pe_exit_price")
    private double peExitPrice;

    /** The status of the strategy execution. */
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status;

    /** The profit or loss from the strategy execution. */
    @Column(name = "pnl")
    private double pnl;

    /** The reason for exiting the strategy. */
    @Column(name = "exit_reason")
    @Enumerated(EnumType.STRING)
    private ExitReason exitReason;

    /** Enum representing the status of the strategy execution. */
    public enum Status {
        ACTIVE, COMPLETED, FAILED
    }

    /** Enum representing the reason for exiting the strategy. */
    public enum ExitReason {
        TARGET_HIT, STOP_LOSS, MANUAL_EXIT, OTHER
    }
}