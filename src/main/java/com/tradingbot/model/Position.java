package com.tradingbot.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Model representing a position in the trading system.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Position {
    /** The trading symbol of the instrument. */
    private String tradingSymbol;

    /** The product type (e.g., MIS, CNC). */
    private String product;

    /** The quantity of the position. */
    private Integer quantity;

    /** The average price of the position. */
    private BigDecimal averagePrice;

    /** The last traded price of the instrument. */
    private BigDecimal lastPrice;

    /** The profit or loss of the position. */
    private BigDecimal pnl;

    /** The unique token identifying the instrument. */
    private String instrumentToken;
}