package com.tradingbot.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "daily_pnl")
public class DailyPnL {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;
    private BigDecimal totalPnl;
    private Integer totalTrades;
    private Boolean tradingStopped;

    // Constructors
    public DailyPnL() {}

    public DailyPnL(LocalDate date) {
        this.date = date;
        this.totalPnl = BigDecimal.ZERO;
        this.totalTrades = 0;
        this.tradingStopped = false;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public BigDecimal getTotalPnl() { return totalPnl; }
    public void setTotalPnl(BigDecimal totalPnl) { this.totalPnl = totalPnl; }
    public Integer getTotalTrades() { return totalTrades; }
    public void setTotalTrades(Integer totalTrades) { this.totalTrades = totalTrades; }
    public Boolean getTradingStopped() { return tradingStopped; }
    public void setTradingStopped(Boolean tradingStopped) { this.tradingStopped = tradingStopped; }
}
