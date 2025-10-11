package com.tradingbot.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

/**
 * Model representing a financial instrument in the trading system.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Instrument {
    /** The trading symbol of the instrument. */
    private String tradingSymbol;

    /** The name of the instrument. */
    private String name;

    /** The last traded price of the instrument. */
    private double lastPrice;

    /** The segment to which the instrument belongs. */
    private String segment;

    /** The exchange where the instrument is traded. */
    private String exchange;

    /** The unique token identifying the instrument. */
    private Integer instrumentToken;

    /** The lot size of the instrument. */
    private Integer lotSize;

    /** The type of the instrument (e.g., FUTURE, OPTION). */
    private String instrumentType;

    /** The strike price of the instrument (if applicable). */
    private double strike;

    /** The expiry date of the instrument (if applicable). */
    private String expiry;
}