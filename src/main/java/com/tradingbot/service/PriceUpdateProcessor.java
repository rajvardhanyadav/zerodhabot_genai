package com.tradingbot.service;

import com.tradingbot.dto.PriceTick;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PriceUpdateProcessor {
    @Autowired
    TradingService tradingService;

    public void processTick(PriceTick tick) {
        // This is where you would implement your logic to find an active
        // trade by its instrument token and check for SL/Target.
        log.info("Processing tick for token {}: LTP = {}", tick.getInstrumentToken(), tick.getLastTradedPrice());
        try {
            tradingService.checkAndClosePositions(tick);
        } catch (Exception e) {
            log.error("Error processing tick for token {}: {}", tick.getInstrumentToken(), e.getMessage());
        }
    }
}