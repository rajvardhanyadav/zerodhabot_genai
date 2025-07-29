package com.tradingbot.service;

import com.tradingbot.entity.DailyPnL;
import com.tradingbot.entity.Trade;
import com.tradingbot.repository.DailyPnLRepository;
import com.tradingbot.repository.TradeRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Value("${trading.strategy.max-daily-loss}")
    private BigDecimal maxDailyLoss;

    @Value("${trading.strategy.profit-target}")
    private BigDecimal profitTarget;

    @Value("${trading.strategy.stop-loss}")
    private BigDecimal stopLoss;

    private boolean tradingActive = false;

    //@Scheduled(cron = "0 */5 9-15 * * MON-FRI") // Every 5 minutes during trading hours
    public void executeStraddleStrategy() {
//        if (!isTradingTime() || !kiteService.isAccessTokenValid() || isTradingStopped()) {
//            return;
//        }

        logger.info("Executing straddle strategy...");

        try {
            // Close existing positions if profit/loss conditions are met
            checkAndClosePositions();

            // Place new straddle if no active positions
            if (getActivePositions().isEmpty()) {
                placeNewStraddle();
            }
        } catch (Exception e) {
            logger.error("Error executing strategy", e);
        }
    }

    private void placeNewStraddle() {
        log.info("placeNewStraddle()");
        List<String> symbols = kiteService.getATMStraddleSymbols();
        if (symbols.size() != 2) {
            logger.error("Unable to get ATM straddle symbols");
            return;
        }

        String ceSymbol = symbols.get(0);
        String peSymbol = symbols.get(1);

        // Get current prices
        BigDecimal cePrice = kiteService.getLastPrice(ceSymbol);
        BigDecimal pePrice = kiteService.getLastPrice(peSymbol);
        log.info("CE price : "+cePrice);
        log.info("PE price : "+pePrice);

        if (cePrice.compareTo(BigDecimal.ZERO) > 0 && pePrice.compareTo(BigDecimal.ZERO) > 0) {
            // Place buy orders for both legs
            String ceOrderId = kiteService.placeOrder(ceSymbol, "BUY", 25, "MARKET", cePrice);
            String peOrderId = kiteService.placeOrder(peSymbol, "BUY", 25, "MARKET", pePrice);

            if (ceOrderId != null && peOrderId != null) {
                // Save trades to database
                Trade ceTrade = new Trade(ceSymbol, "BUY", 25, cePrice, "STRADDLE");
                ceTrade.setOrderId(ceOrderId);
                Trade peTrade = new Trade(peSymbol, "BUY", 25, pePrice, "STRADDLE");
                peTrade.setOrderId(peOrderId);

                tradeRepository.save(ceTrade);
                tradeRepository.save(peTrade);

                logger.info("Placed straddle: CE {} at {}, PE {} at {}", ceSymbol, cePrice, peSymbol, pePrice);

                // Send update via WebSocket
                messagingTemplate.convertAndSend("/topic/trades", "New straddle placed");
            }
        }
    }

    private void checkAndClosePositions() {
        List<Trade> activeTrades = getActivePositions();

        for (Trade trade : activeTrades) {
            BigDecimal currentPrice = kiteService.getLastPrice(trade.getSymbol());
            BigDecimal priceDiff = currentPrice.subtract(trade.getPrice());

            boolean shouldClose = false;

            // Check profit target (30 points up)
            if (priceDiff.compareTo(profitTarget) >= 0) {
                shouldClose = true;
                logger.info("Profit target hit for {}: {} -> {}", trade.getSymbol(), trade.getPrice(), currentPrice);
            }

            // Check stop loss (15 points down)
            if (priceDiff.compareTo(stopLoss.negate()) <= 0) {
                shouldClose = true;
                logger.info("Stop loss hit for {}: {} -> {}", trade.getSymbol(), trade.getPrice(), currentPrice);
            }

            if (shouldClose) {
                String orderId = kiteService.placeOrder(trade.getSymbol(), "SELL", trade.getQuantity(), "MARKET", currentPrice);
                if (orderId != null) {
                    trade.setStatus("CLOSED");
                    trade.setPnl(priceDiff.multiply(new BigDecimal(trade.getQuantity())));
                    tradeRepository.save(trade);

                    updateDailyPnL(trade.getPnl());

                    // Close the other leg of straddle
                    closeOtherLeg(trade);
                }
            }
        }
    }

    private void closeOtherLeg(Trade closedTrade) {
        List<Trade> activeTrades = tradeRepository.findByStrategyAndStatus("STRADDLE", "OPEN");

        for (Trade trade : activeTrades) {
            if (!trade.getId().equals(closedTrade.getId()) &&
                    trade.getTimestamp().isAfter(closedTrade.getTimestamp().minusMinutes(1))) {

                BigDecimal currentPrice = kiteService.getLastPrice(trade.getSymbol());
                String orderId = kiteService.placeOrder(trade.getSymbol(), "SELL", trade.getQuantity(), "MARKET", currentPrice);

                if (orderId != null) {
                    trade.setStatus("CLOSED");
                    BigDecimal pnl = currentPrice.subtract(trade.getPrice()).multiply(new BigDecimal(trade.getQuantity()));
                    trade.setPnl(pnl);
                    tradeRepository.save(trade);

                    updateDailyPnL(pnl);

                    logger.info("Closed other leg: {} at {} with PnL: {}", trade.getSymbol(), currentPrice, pnl);
                }
                break;
            }
        }
    }

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
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay(); // Today at 00:00
        LocalDateTime endOfDay = LocalDate.now().plusDays(1).atStartOfDay();
        BigDecimal todaysPnL = tradeRepository.findTodaysPnLBetween(startOfDay, endOfDay);
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

        // Send update via WebSocket
        messagingTemplate.convertAndSend("/topic/pnl", dailyPnL);
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
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay(); // Today at 00:00
        LocalDateTime endOfDay = LocalDate.now().plusDays(1).atStartOfDay();
        return tradeRepository.findTodaysPnLBetween(startOfDay, endOfDay);
    }
}
