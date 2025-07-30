package com.tradingbot.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;
    private String type; // BUY/SELL
    private Integer quantity;

    // Renamed from 'price' for clarity
    @Column(name = "entry_price")
    private BigDecimal entryPrice;

    // --- NEW FIELD ---
    // To store the price at which the position was closed
    @Column(name = "exit_price")
    private BigDecimal exitPrice;

    private BigDecimal pnl;
    private String status; // OPEN/CLOSED

    // Renamed from 'timestamp' for clarity
    @Column(name = "entry_timestamp")
    private LocalDateTime entryTimestamp;

    // --- NEW FIELD ---
    // To store the time at which the position was closed
    @Column(name = "exit_timestamp")
    private LocalDateTime exitTimestamp;

    private String orderId;
    private String strategy;
    private String instrumentToken;

    // Constructors
    public Trade() {}

    // Updated constructor to set the entry price and timestamp
    public Trade(String symbol, String type, Integer quantity, BigDecimal entryPrice, String strategy) {
        this.symbol = symbol;
        this.type = type;
        this.quantity = quantity;
        this.entryPrice = entryPrice;
        this.strategy = strategy;
        this.status = "OPEN";
        this.entryTimestamp = LocalDateTime.now();
        this.pnl = BigDecimal.ZERO;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getEntryPrice() { return entryPrice; }
    public void setEntryPrice(BigDecimal entryPrice) { this.entryPrice = entryPrice; }
    public BigDecimal getExitPrice() { return exitPrice; }
    public void setExitPrice(BigDecimal exitPrice) { this.exitPrice = exitPrice; }
    public BigDecimal getPnl() { return pnl; }
    public void setPnl(BigDecimal pnl) { this.pnl = pnl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getEntryTimestamp() { return entryTimestamp; }
    public void setEntryTimestamp(LocalDateTime entryTimestamp) { this.entryTimestamp = entryTimestamp; }
    public LocalDateTime getExitTimestamp() { return exitTimestamp; }
    public void setExitTimestamp(LocalDateTime exitTimestamp) { this.exitTimestamp = exitTimestamp; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }
    public String getInstrumentToken() { return instrumentToken; }
    public void setInstrumentToken(String instrumentToken) { this.instrumentToken = instrumentToken; }
}