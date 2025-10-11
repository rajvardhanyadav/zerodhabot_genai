package com.tradingbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing a paper trade for simulation purposes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public final class PaperTrade {
    private String orderId;
    private String symbol;
    private TransactionType transactionType; // BUY or SELL
    private int quantity;
    private OrderType orderType; // MARKET, LIMIT
    private BigDecimal orderPrice; // Price specified in the order
    private BigDecimal executionPrice; // Price at which order was executed
    private BigDecimal marketPrice; // Market price at time of order
    private BigDecimal currentPrice; // Current market price (updated)
    private BigDecimal pnl = BigDecimal.ZERO; // Current P&L
    private LocalDateTime orderTime = LocalDateTime.now();
    private LocalDateTime lastUpdated = LocalDateTime.now();
    private Status status = Status.PENDING; // COMPLETE, PENDING, CANCELLED
    private String notes; // Optional notes

    public enum TransactionType {
        BUY, SELL
    }

    public enum OrderType {
        MARKET, LIMIT
    }

    public enum Status {
        COMPLETE, PENDING, CANCELLED
    }
}