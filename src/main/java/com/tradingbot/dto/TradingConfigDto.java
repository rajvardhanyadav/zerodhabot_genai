package com.tradingbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A Data Transfer Object (DTO) representing trading configuration settings.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public final class TradingConfigDto {
    /** The expiry date for the trading configuration. */
    private LocalDate expiryDate;

    /** The maximum daily loss allowed. */
    private BigDecimal maxDailyLoss;

    /** The profit target for the trading session. */
    private BigDecimal profitTarget;

    /** The stop-loss limit for the trading session. */
    private BigDecimal stopLoss;
}