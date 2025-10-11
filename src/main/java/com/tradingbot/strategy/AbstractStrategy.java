package com.tradingbot.strategy;

import com.tradingbot.dto.TradingConfigDto;
import com.tradingbot.entity.Trade;
import com.tradingbot.service.KiteService;
import com.tradingbot.service.TradingService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public abstract class AbstractStrategy implements Strategy {
    protected KiteService kiteService;
    protected TradingService tradingService;

//    protected List<Trade> activeTrades = new ArrayList<>();
    private BigDecimal profitTarget;
    private BigDecimal stopLoss;
    private LocalDate selectedExpiryDate;

    protected AbstractStrategy(KiteService kiteService, TradingService tradingService) {
        this.kiteService=kiteService;
        this.tradingService=tradingService;
    }

    protected boolean isTradingTime() {
        return this.tradingService.isTradingTime();
    }

    protected boolean isTradingStopped() {
        return this.tradingService.isTradingStopped();
    }

    protected boolean isAccessTokenValid(){
        return kiteService.isAccessTokenValid();

    }

    List<Trade> getActivePositions() {
        return tradingService.getActivePositions();
    }

    protected BigDecimal getLTP(String symbol){
        return kiteService.getLastPrice(symbol);
    }

    protected void loadTradingConfig(TradingConfigDto config){
        this.profitTarget = config.getProfitTarget();
        this.stopLoss = config.getStopLoss();
        this.selectedExpiryDate=config.getExpiryDate();
        tradingService.loadTradingConfig(config);
    }

    protected BigDecimal getProfitTarget() {
        return profitTarget;
    }

    protected BigDecimal getStopLoss() {
        return stopLoss;
    }

    protected LocalDate getSelectedExpiryDate(){
        return selectedExpiryDate;
    }
}
