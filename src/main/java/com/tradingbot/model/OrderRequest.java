package com.tradingbot.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;

import java.math.BigDecimal;

/**
 * Model representing an order request in the trading system.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderRequest {
    /** The variety of the order (e.g., regular, AMO). */
    @Default
    private String variety = "regular";

    /** The exchange where the order is placed (e.g., NFO, NSE). */
    @Default
    private String exchange = "NFO";

    /** The trading symbol of the instrument. */
    private String tradingSymbol;

    /** The type of transaction (e.g., BUY, SELL). */
    private String transactionType;

    /** The quantity of the order. */
    private Integer quantity;

    /** The product type (e.g., MIS, CNC). */
    @Default
    private String product = "MIS";

    /** The order type (e.g., MARKET, LIMIT). */
    @Default
    private String orderType = "MARKET";

    /** The price of the order. */
    @Default
    private BigDecimal price = BigDecimal.ZERO;

    /** The validity of the order (e.g., DAY, IOC). */
    @Default
    private String validity = "DAY";
}