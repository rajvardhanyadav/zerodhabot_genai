package com.tradingbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "strategy_executions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StrategyExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "strategy_id", unique = true)
    private String strategyId;

    @Column(name = "entry_time")
    private LocalDateTime entryTime;

    @Column(name = "exit_time")
    private LocalDateTime exitTime;

    @Column(name = "atm_strike")
    private double atmStrike;

    @Column(name = "ce_symbol")
    private String ceSymbol;

    @Column(name = "pe_symbol")
    private String peSymbol;

    @Column(name = "ce_entry_price")
    private double ceEntryPrice;

    @Column(name = "pe_entry_price")
    private double peEntryPrice;

    @Column(name = "ce_exit_price")
    private double ceExitPrice;

    @Column(name = "pe_exit_price")
    private double peExitPrice;

    @Column(name = "status")
    private String status; // ACTIVE, COMPLETED, FAILED

    @Column(name = "pnl")
    private double pnl;

    @Column(name = "exit_reason")
    private String exitReason;
}
