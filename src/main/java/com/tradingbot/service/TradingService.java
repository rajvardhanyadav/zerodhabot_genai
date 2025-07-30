package com.tradingbot.service;

import com.tradingbot.dto.NfoInstrument;
import com.tradingbot.dto.PriceTick;
import com.tradingbot.entity.DailyPnL;
import com.tradingbot.entity.Trade;
import com.tradingbot.event.PriceTickEvent;
import com.tradingbot.repository.DailyPnLRepository;
import com.tradingbot.repository.TradeRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TradingService {
    private static final Logger logger = LoggerFactory.getLogger(TradingService.class);

    @Autowired
    private KiteService kiteService;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private DailyPnLRepository dailyPnLRepository;

    @Value("${trading.strategy.max-daily-loss}")
    private BigDecimal maxDailyLoss;

    @Value("${trading.strategy.profit-target}")
    private BigDecimal profitTarget;

    @Value("${trading.strategy.stop-loss}")
    private BigDecimal stopLoss;

    private boolean tradingActive = false;

    @Autowired
    KiteTickerService kiteTickerService;

    List<Trade> activeTrades =new ArrayList<>();

    // No changes in this section (executeStraddleStrategy, placeNewStraddle)
    // The constructor for Trade already handles setting the entryPrice.
    @Scheduled(cron = "0 */5 9-15 * * MON-FRI")
    public void executeStraddleStrategy() {
        if (!isTradingTime() || !kiteService.isAccessTokenValid() || isTradingStopped()) {
            return;
        }
        logger.info("Executing straddle strategy...");
        try {
            if (getActivePositions().isEmpty()) {
                placeNewStraddle();
            }
        } catch (Exception e) {
            logger.error("Error executing strategy", e);
        }
    }

    private void placeNewStraddle() {
        log.info("placeNewStraddle()");
        List<NfoInstrument> symbols = kiteService.getATMStraddleSymbols();
        if (symbols.size() != 2) {
            logger.error("Unable to get ATM straddle symbols");
            return;
        }
        String ceSymbol = symbols.get(0).getTradingsymbol();
        String peSymbol = symbols.get(1).getTradingsymbol();
        BigDecimal cePrice = kiteService.getLastPrice(ceSymbol);
        BigDecimal pePrice = kiteService.getLastPrice(peSymbol);
        log.info("CE price : " + cePrice);
        log.info("PE price : " + pePrice);
        if (cePrice.compareTo(BigDecimal.ZERO) > 0 && pePrice.compareTo(BigDecimal.ZERO) > 0) {
            String ceOrderId = kiteService.placeOrder(ceSymbol, "BUY", 25, "MARKET", cePrice);
            String peOrderId = kiteService.placeOrder(peSymbol, "BUY", 25, "MARKET", pePrice);
            if (ceOrderId != null && peOrderId != null) {
                // The updated Trade constructor correctly sets the entryPrice
                Trade ceTrade = new Trade(ceSymbol, "BUY", 25, cePrice, "STRADDLE");
                ceTrade.setOrderId(ceOrderId);
                ceTrade.setInstrumentToken(symbols.get(0).getInstrument_token());
                Trade peTrade = new Trade(peSymbol, "BUY", 25, pePrice, "STRADDLE");
                peTrade.setOrderId(peOrderId);
                peTrade.setInstrumentToken(symbols.get(1).getInstrument_token());
                tradeRepository.save(ceTrade);
                tradeRepository.save(peTrade);
                logger.info("Placed straddle: CE {} at {}, PE {} at {}", ceSymbol, cePrice, peSymbol, pePrice);
                List<Long> tokens = new ArrayList<>();
                tokens.add(Long.valueOf(symbols.get(0).getInstrument_token()));
                tokens.add(Long.valueOf(symbols.get(1).getInstrument_token()));
                kiteTickerService.subscribe(tokens);
                activeTrades = getActivePositions();
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

    void checkAndClosePositions(PriceTick tick) {
        log.info("activeTrades.size() : "+activeTrades.size());
        if (activeTrades.isEmpty()) {
            return;
        }

        for (Trade trade : activeTrades) {
            if (Long.parseLong(trade.getInstrumentToken()) != tick.getInstrumentToken()) {
                continue;
            }

            BigDecimal currentPrice = BigDecimal.valueOf(tick.getLastTradedPrice());
            // --- CHANGE: Use getEntryPrice() for PnL calculation ---
            BigDecimal priceDiff = currentPrice.subtract(trade.getEntryPrice());

            boolean shouldClose = false;

            if (priceDiff.compareTo(profitTarget) >= 0) {
                shouldClose = true;
                // --- CHANGE: Use getEntryPrice() for logging ---
                logger.info("Profit target hit for {}: {} -> {}", trade.getSymbol(), trade.getEntryPrice(), currentPrice);
            }
            if (priceDiff.compareTo(stopLoss.negate()) <= 0) {
                shouldClose = true;
                // --- CHANGE: Use getEntryPrice() for logging ---
                logger.info("Stop loss hit for {}: {} -> {}", trade.getSymbol(), trade.getEntryPrice(), currentPrice);
            }

            if (shouldClose) {
                String orderId = kiteService.placeOrder(trade.getSymbol(), "SELL", trade.getQuantity(), "MARKET", currentPrice);
                if (orderId != null) {
                    // --- CHANGE: Set new exit fields before saving ---
                    trade.setStatus("CLOSED");
                    trade.setExitPrice(currentPrice); // Set the exit price
                    trade.setExitTimestamp(LocalDateTime.now()); // Set the exit time
                    trade.setPnl(priceDiff.multiply(new BigDecimal(trade.getQuantity())));
                    tradeRepository.save(trade);

                    updateDailyPnL(trade.getPnl());
                    closeOtherLeg(trade);
                    break;
                }
            }
        }
    }

    private void closeOtherLeg(Trade closedTrade) {
//        List<Trade> activeTrades = tradeRepository.findByStrategyAndStatus("STRADDLE", "OPEN");

        for (Trade trade : activeTrades) {
            // --- CHANGE: Use getEntryTimestamp() for comparison ---
            if (!trade.getId().equals(closedTrade.getId()) &&
                    trade.getEntryTimestamp().isAfter(closedTrade.getEntryTimestamp().minusMinutes(1))) {

                BigDecimal currentPrice = kiteService.getLastPrice(trade.getSymbol());
                String orderId = kiteService.placeOrder(trade.getSymbol(), "SELL", trade.getQuantity(), "MARKET", currentPrice);

                if (orderId != null) {
                    // --- CHANGE: Use getEntryPrice() and set new exit fields ---
                    BigDecimal pnl = currentPrice.subtract(trade.getEntryPrice()).multiply(new BigDecimal(trade.getQuantity()));

                    trade.setStatus("CLOSED");
                    trade.setExitPrice(currentPrice); // Set the exit price
                    trade.setExitTimestamp(LocalDateTime.now()); // Set the exit time
                    trade.setPnl(pnl);
                    tradeRepository.save(trade);

                    updateDailyPnL(pnl);
                    logger.info("Closed other leg: {} at {} with PnL: {}", trade.getSymbol(), currentPrice, pnl);
                }
                break;
            }
        }
    }

    // No changes needed in the methods below
    private List<Trade> getActivePositions() {
        return tradeRepository.findByStatus("OPEN");
    }

    private boolean isTradingTime() {
        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.of(9, 15);
        LocalTime end = LocalTime.of(15, 30);
        return now.isAfter(start) && now.isBefore(end);
    }

    private boolean isTradingStopped() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().plusDays(1).atStartOfDay();
        BigDecimal todaysPnL = tradeRepository.findTodaysPnLBetween(startOfDay, endOfDay);
        if (todaysPnL == null) {
            todaysPnL = BigDecimal.ZERO;
        }
        return todaysPnL.compareTo(maxDailyLoss.negate()) <= 0;
    }

    private void updateDailyPnL(BigDecimal pnl) {
        LocalDate today = LocalDate.now();
        DailyPnL dailyPnL = dailyPnLRepository.findByDate(today)
                .orElse(new DailyPnL(today));
        dailyPnL.setTotalPnl(dailyPnL.getTotalPnl().add(pnl));
        dailyPnL.setTotalTrades(dailyPnL.getTotalTrades() + 1);
        if (dailyPnL.getTotalPnl().compareTo(maxDailyLoss.negate()) <= 0) {
            dailyPnL.setTradingStopped(true);
            logger.warn("Daily loss limit reached. Trading stopped for today.");
        }
        dailyPnLRepository.save(dailyPnL);
    }

    public void startTrading() {
        tradingActive = true;
        logger.info("Trading started");
        executeStraddleStrategy();
    }

    public void stopTrading() {
        tradingActive = false;
        logger.info("Trading stopped");
    }

    public boolean isTradingActive() {
        log.info("Checking trading status");
        return tradingActive;
    }

    public BigDecimal getTodaysPnL() {
        log.info("Fetching todays PnL");
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().plusDays(1).atStartOfDay();
        return tradeRepository.findTodaysPnLBetween(startOfDay, endOfDay);
    }
}