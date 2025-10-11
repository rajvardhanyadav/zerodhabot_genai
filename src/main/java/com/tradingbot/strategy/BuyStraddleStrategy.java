package com.tradingbot.strategy;

import com.tradingbot.dto.NfoInstrument;
import com.tradingbot.dto.PriceTick;
import com.tradingbot.dto.TradingConfigDto;
import com.tradingbot.entity.Trade;
import com.tradingbot.event.PriceTickEvent;
import com.tradingbot.service.KiteService;
import com.tradingbot.service.TradingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class BuyStraddleStrategy extends AbstractStrategy {

    protected BuyStraddleStrategy(KiteService kiteService, TradingService tradingService) {
        super(kiteService, tradingService);
    }

    @Override
    public StrategyType getType() {
        return StrategyType.BUY_STRADDLE;
    }

    @Override
    public void execute(TradingConfigDto config) {
        loadTradingConfig(config);
        executeStrategy();
    }

    @Scheduled(cron = "0 */5 9-23 * * MON-SAT")
    private void executeStrategy() {
        log.info("executeStrategy()");
        //        if (!isTradingTime() || !isAccessTokenValid() || isTradingStopped()) {
//            return;
//        }
        log.info("Executing straddle strategy...");
        try {
            if (getActivePositions().isEmpty()) {
                placeNewStraddle();
            }
        } catch (Exception e) {
            log.error("Error executing strategy", e);
        }
    }

    private void placeNewStraddle() {
        log.info("Placing new straddle...");
        List<NfoInstrument> symbols = kiteService.getATMStraddleSymbols(getSelectedExpiryDate());
        if (symbols.size() != 2) {
            log.error("Unable to get ATM straddle symbols");
            return;
        }

        String ceSymbol = symbols.get(0).getTradingSymbol();
        String peSymbol = symbols.get(1).getTradingSymbol();
        BigDecimal cePrice = getLTP(ceSymbol);
        BigDecimal pePrice = getLTP(peSymbol);

        if (cePrice.compareTo(BigDecimal.ZERO) > 0 && pePrice.compareTo(BigDecimal.ZERO) > 0) {
            String ceOrderId = tradingService.placeOrder(ceSymbol, TradingService.TRANSACTION_BUY, cePrice);
            String peOrderId = tradingService.placeOrder(peSymbol, TradingService.TRANSACTION_BUY, pePrice);

            if (ceOrderId != null && peOrderId != null) {
                tradingService.saveTrade(ceSymbol, TradingService.TRANSACTION_BUY, ceOrderId, cePrice, symbols.get(0).getInstrumentToken());
                tradingService.saveTrade(peSymbol, TradingService.TRANSACTION_BUY, peOrderId, pePrice, symbols.get(1).getInstrumentToken());
                tradingService.subscribeToTokens(symbols);
                getActivePositions();
            }
        }
    }

    @EventListener
    public void handlePriceTickEvent(PriceTickEvent event) {
        PriceTick tick = event.getPriceTick();
        try {
            checkAndClosePositions(tick);
        } catch (Exception e) {
            log.error("Error processing tick for token {}: {}", tick.getInstrumentToken(), e.getMessage(), e);
        }
    }

    public void checkAndClosePositions(PriceTick tick) {
        log.info("Checking positions for token: {}", tick.getInstrumentToken());
        if (getActivePositions().isEmpty()) {
            return;
        }

        for (Trade trade : getActivePositions()) {
            if (!trade.getInstrumentToken().equals(String.valueOf(tick.getInstrumentToken()))) {
                continue;
            }

            BigDecimal currentPrice = BigDecimal.valueOf(tick.getLastTradedPrice());
            BigDecimal priceDiff = currentPrice.subtract(trade.getEntryPrice());
            log.info("currentPrice : "+currentPrice);
            log.info("priceDiff : "+priceDiff);

            if (shouldClosePosition(priceDiff)) {
                log.info("Closing trade for symbol: {}", trade.getSymbol());
                log.info("Total active trades before closing: {}", getActivePositions().size());
                tradingService.closeTrade(trade, TradingService.TRANSACTION_SELL, currentPrice, priceDiff);
                closeOtherLeg(trade);
                break;
            }
        }
    }

    private void closeOtherLeg(Trade closedTrade) {
        log.info("Closing other leg of the straddle for trade ID: {}", closedTrade.getId());
        log.info("Total active trades before closing: {}", getActivePositions().size());
        for (Trade trade : getActivePositions()) {
            if (!trade.getId().equals(closedTrade.getId()) &&
                    trade.getEntryTimestamp().isAfter(closedTrade.getEntryTimestamp().minusMinutes(1))) {

                BigDecimal currentPrice = kiteService.getLastPrice(trade.getSymbol());
                tradingService.closeTrade(trade, TradingService.TRANSACTION_SELL, currentPrice, currentPrice.subtract(trade.getEntryPrice()));
                break;
            }
        }
    }

    private boolean shouldClosePosition(BigDecimal priceDiff) {
        return priceDiff.compareTo(getProfitTarget()) >= 0 || priceDiff.compareTo(getStopLoss().negate()) <= 0;
    }
}
