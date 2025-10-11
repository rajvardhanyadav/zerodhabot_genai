package com.tradingbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A Data Transfer Object (DTO) representing a single price tick
 * received from the WebSocket API.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public final class PriceTick {
    /** The unique token identifying the instrument. */
    private long instrumentToken;

    /** The last traded price of the instrument. */
    private double lastTradedPrice;
}