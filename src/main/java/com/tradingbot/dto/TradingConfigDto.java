// Create a new file: TradingConfigDto.java
package com.tradingbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradingConfigDto {
    private LocalDate expiryDate;
    private BigDecimal maxDailyLoss;
    private BigDecimal profitTarget;
    private BigDecimal stopLoss;

    // Add Getters and Setters for all fields
}