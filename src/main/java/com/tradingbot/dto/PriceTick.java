package com.tradingbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A Data Transfer Object (DTO) representing a single price tick
 * received from the WebSocket API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceTick {
    private long instrumentToken;
    private double lastTradedPrice;
}