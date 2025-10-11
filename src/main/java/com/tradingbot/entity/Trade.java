package com.tradingbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a trade in the trading system.
 */
@Entity
@Table(name = "trades")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public final class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The symbol of the traded instrument. */
    private String symbol;

    /** The type of trade (BUY/SELL). */
    @Enumerated(EnumType.STRING)
    private TradeType type;

    /** The quantity of the trade. */
    private Integer quantity;

    /** The price at which the position was entered. */
    @Column(name = "entry_price")
    private BigDecimal entryPrice;

    /** The price at which the position was closed. */
    @Column(name = "exit_price")
    private BigDecimal exitPrice;

    /** The profit or loss of the trade. */
    @Builder.Default
    private BigDecimal pnl = BigDecimal.ZERO;

    /** The status of the trade (OPEN/CLOSED). */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TradeStatus status = TradeStatus.OPEN;

    /** The timestamp when the position was entered. */
    @Column(name = "entry_timestamp")
    @Builder.Default
    private LocalDateTime entryTimestamp = LocalDateTime.now();

    /** The timestamp when the position was closed. */
    @Column(name = "exit_timestamp")
    private LocalDateTime exitTimestamp;

    /** The order ID associated with the trade. */
    private String orderId;

    /** The strategy used for the trade. */
    private String strategy;

    /** The instrument token of the traded instrument. */
    private String instrumentToken;

    /** Enum representing the type of trade. */
    public enum TradeType {
        BUY, SELL
    }

    /** Enum representing the status of the trade. */
    public enum TradeStatus {
        OPEN, CLOSED
    }

    public Trade(String symbol, String type, int quantity, BigDecimal entryPrice, String strategy) {
        this.symbol = symbol;
        this.type = TradeType.valueOf(type); // Convert String to Enum
        this.quantity = quantity;
        this.entryPrice = entryPrice;
        this.strategy = strategy;
        this.status = TradeStatus.OPEN; // Default status
        this.entryTimestamp = LocalDateTime.now(); // Default entry timestamp
    }
}