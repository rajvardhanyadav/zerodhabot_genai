package com.tradingbot.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaperTrade {
    private String orderId;
    private String symbol;
    private String transactionType; // BUY or SELL
    private int quantity;
    private String orderType; // MARKET, LIMIT
    private BigDecimal orderPrice; // Price specified in the order
    private BigDecimal executionPrice; // Price at which order was executed
    private BigDecimal marketPrice; // Market price at time of order
    private BigDecimal currentPrice; // Current market price (updated)
    private BigDecimal pnl; // Current P&L
    private LocalDateTime orderTime;
    private LocalDateTime lastUpdated;
    private String status; // COMPLETE, PENDING, CANCELLED
    private String notes; // Optional notes

    public PaperTrade() {
        this.pnl = BigDecimal.ZERO;
        this.status = "PENDING";
        this.orderTime = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }
}