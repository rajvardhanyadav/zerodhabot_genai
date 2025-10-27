package com.tradingbot.service;

import com.tradingbot.dto.NfoInstrument;
import com.tradingbot.dto.PriceTick;
import com.tradingbot.dto.TradingConfigDto;
import com.tradingbot.entity.DailyPnL;
import com.tradingbot.entity.Trade;
import com.tradingbot.event.PriceTickEvent;
import com.tradingbot.repository.DailyPnLRepository;
import com.tradingbot.repository.TradeRepository;
import com.zerodhatech.models.Order;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class TradingService {

    private static final Trade.TradeStatus STATUS_OPEN = Trade.TradeStatus.OPEN;
    private static final String STRATEGY_STRADDLE = "STRADDLE";
    public static final String TRANSACTION_BUY = "BUY";
    public static final String TRANSACTION_SELL = "SELL";

    private final KiteService kiteService;
    private final TradeRepository tradeRepository;
    private final DailyPnLRepository dailyPnLRepository;
    private final KiteTickerService kiteTickerService;

    @Value("${trading.strategy.max-daily-loss}")
    private BigDecimal maxDailyLoss;

    @Value("${trading.strategy.profit-target}")
    private BigDecimal profitTarget;

    @Value("${trading.strategy.stop-loss}")
    private BigDecimal stopLoss;

    private LocalDate selectedExpiryDate;
    @Getter
    private boolean tradingActive = false;
    private List<Trade> activeTrades = new ArrayList<>();

    public TradingService(KiteService kiteService, TradeRepository tradeRepository,
                          DailyPnLRepository dailyPnLRepository, KiteTickerService kiteTickerService) {
        this.kiteService = kiteService;
        this.tradeRepository = tradeRepository;
        this.dailyPnLRepository = dailyPnLRepository;
        this.kiteTickerService = kiteTickerService;
    }

    public String placeOrder(String symbol, String transactionType, BigDecimal price) {
        return kiteService.placeOrder(symbol, transactionType, 35, "MARKET", price);
    }

    public void saveTrade(String symbol, String transactionType, String orderId, BigDecimal price, String instrumentToken, Trade.TradeStatus status) {
        if(orderId == null){
            log.error("Order ID is null, cannot save trade for symbol: {}", symbol);
            return;
        }
        Optional<Trade> maybeTrade = tradeRepository.findByOrderId(orderId);
        if (maybeTrade.isEmpty()) {
            Trade trade = new Trade(symbol, transactionType, 35, price, STRATEGY_STRADDLE, status);
            trade.setOrderId(orderId);
            trade.setInstrumentToken(instrumentToken);
            tradeRepository.save(trade);
            log.info("Trade saved: {} at {}. OrderId: {}. Status: {}", symbol, price, orderId, status);
        }
    }



    public void subscribeToTokens(List<NfoInstrument> symbols) {
        List<Long> tokens = new ArrayList<>();
        tokens.add(Long.valueOf(symbols.get(0).getInstrumentToken()));
        tokens.add(Long.valueOf(symbols.get(1).getInstrumentToken()));
        kiteTickerService.subscribe(tokens);
//        CsvFeedSimulator csvFeedSimulator = new CsvFeedSimulator(kiteTickerService);
//        csvFeedSimulator.simulateFeedAsync();
    }

    private void unsubscribeFromTokens(String symbols) {
//        List<Long> tokens = new ArrayList<>();
//        tokens.add(Long.valueOf(symbols));
        kiteTickerService.unsubscribe();
    }

    public void closeTrade(Trade trade, String transactionType, BigDecimal currentPrice, BigDecimal priceDiff) {
        log.info("Placing sell order to close trade for symbol: {}", trade.getSymbol());
        String orderId = kiteService.placeOrder(trade.getSymbol(), TRANSACTION_SELL, trade.getQuantity(), "MARKET", currentPrice);
        if (orderId != null) {
            trade.setStatus(Trade.TradeStatus.COMPLETE);
            trade.setExitPrice(currentPrice);
            trade.setExitTimestamp(LocalDateTime.now());
            trade.setPnl(priceDiff.multiply(new BigDecimal(trade.getQuantity())));
            tradeRepository.save(trade);

            updateDailyPnL(trade.getPnl());
            activeTrades = getActivePositions();
            unsubscribeFromTokens(trade.getSymbol());
            log.info("Active trades remaining: {}", activeTrades.size());
        }
    }

    public List<Trade> getActivePositions() {
        return tradeRepository.findByStatus(STATUS_OPEN);
    }

    public boolean isTradingTime() {
        LocalTime now = LocalTime.now();
        return now.isAfter(LocalTime.of(9, 15)) && now.isBefore(LocalTime.of(16, 30));
    }

    public boolean isTradingStopped() {
        return false;
        /*LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().plusDays(1).atStartOfDay();
        BigDecimal todaysPnL = Optional.of(tradeRepository.findTodaysPnLBetween(startOfDay, endOfDay))
                .orElse(BigDecimal.ZERO);
        return todaysPnL.compareTo(maxDailyLoss.negate()) <= 0;*/
    }

    private void updateDailyPnL(BigDecimal pnl) {
        LocalDate today = LocalDate.now();
        DailyPnL dailyPnL = dailyPnLRepository.findByDate(today).orElse(new DailyPnL(today));
        dailyPnL.setTotalPnl(dailyPnL.getTotalPnl().add(pnl));
        dailyPnL.setTotalTrades(dailyPnL.getTotalTrades() + 1);
        if (dailyPnL.getTotalPnl().compareTo(maxDailyLoss.negate()) <= 0) {
            dailyPnL.setTradingStopped(true);
            log.warn("Daily loss limit reached. Trading stopped for today.");
        }
        dailyPnLRepository.save(dailyPnL);
    }

    public void startTrading() {
        tradingActive = true;
        log.info("Trading started");
    }

    public void stopTrading() {
        tradingActive = false;
        unsubscribeFromTokens("123");
        log.info("Trading stopped");
    }

    public BigDecimal getTodaysPnL() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().plusDays(1).atStartOfDay();
        return tradeRepository.findTodaysPnLBetween(startOfDay, endOfDay);
    }

    public void loadTradingConfig(TradingConfigDto config) {
        tradingActive = true;
        this.maxDailyLoss = config.getMaxDailyLoss();
        this.profitTarget = config.getProfitTarget();
        this.stopLoss = config.getStopLoss();
        this.selectedExpiryDate = config.getExpiryDate();
    }

    @EventListener
    public void handleOrderUpdate(Order order) {
        log.info("Order Update Received: {}", order.tradingSymbol+" "+order.status);
        updateTrade(order);
//        try {
//            checkAndClosePositions(tick);
//        } catch (Exception e) {
//            log.error("Error processing tick for token {}: {}", tick.getInstrumentToken(), e.getMessage(), e);
//        }
    }

    /**
     * Update a Trade entity based on an incoming Kite Order update.
     * The method is resilient to different Order model field/getter names by using reflection.
     */
    private void updateTrade(Order order) {
        if (order == null) return;

        String orderId = null;
        // Try common field names using reflection
        try {
            Field f = order.getClass().getDeclaredField("order_id");
            f.setAccessible(true);
            Object v = f.get(order);
            if (v != null) orderId = v.toString();
        } catch (Exception ignored) {}

        if (orderId == null) {
            try {
                Field f = order.getClass().getDeclaredField("orderId");
                f.setAccessible(true);
                Object v = f.get(order);
                if (v != null) orderId = v.toString();
            } catch (Exception ignored) {}
        }

        // Try getter as a last resort
        if (orderId == null) {
            try {
                Method m = order.getClass().getMethod("getOrderId");
                Object v = m.invoke(order);
                if (v != null) orderId = v.toString();
            } catch (Exception ignored) {}
        }

        if (orderId == null) {
            log.warn("Received order update but could not extract order id from Order model: {}", order);
            return;
        }

        // Extract status string from the Order
        String orderStatus = null;
        try {
            Field sf = order.getClass().getDeclaredField("status");
            sf.setAccessible(true);
            Object sv = sf.get(order);
            if (sv != null) orderStatus = sv.toString();
        } catch (Exception ignored) {}

        if (orderStatus == null) {
            try {
                Method ms = order.getClass().getMethod("getStatus");
                Object sv = ms.invoke(order);
                if (sv != null) orderStatus = sv.toString();
            } catch (Exception ignored) {}
        }

        Trade.TradeStatus mappedStatus = Trade.TradeStatus.OPEN;
        if (orderStatus != null) {
            String s = orderStatus.toUpperCase();
            if (s.contains("COMPLETE") || s.contains("FILLED") || s.contains("TRIGGERED")) {
                mappedStatus = Trade.TradeStatus.COMPLETE;
            } else if (s.contains("CANCEL")) {
                mappedStatus = Trade.TradeStatus.CANCELLED;
            } else if (s.contains("REJECT")) {
                mappedStatus = Trade.TradeStatus.REJECTED;
            } else if (s.contains("TRIGGER")) {
                mappedStatus = Trade.TradeStatus.TRIGGER_PENDING;
            } else if (s.contains("PEND")) {
                mappedStatus = Trade.TradeStatus.PENDING;
            } else {
                mappedStatus = Trade.TradeStatus.OPEN;
            }
        }

        Optional<Trade> maybeTrade = tradeRepository.findByOrderId(orderId);
        if (maybeTrade.isEmpty()) {
            log.warn("Order update for unknown orderId {} (status={}). Ignoring.", orderId, orderStatus);
            saveTrade(order.tradingSymbol, order.transactionType, orderId, new BigDecimal(order.price), order.tradingSymbol, mappedStatus);
            return;
        }

        Trade trade = maybeTrade.get();
        trade.setStatus(mappedStatus);

        // If order completed, attempt to set exit price and pnl
        if (mappedStatus == Trade.TradeStatus.COMPLETE) {
            trade.setExitTimestamp(LocalDateTime.now());

            // Try to read execution/average price from order object
            BigDecimal execPrice = null;
            try {
                Field pf = order.getClass().getDeclaredField("average_price");
                pf.setAccessible(true);
                Object pv = pf.get(order);
                if (pv != null) execPrice = toBigDecimal(pv);
            } catch (Exception ignored) {}

            if (execPrice == null) {
                try {
                    Field pf = order.getClass().getDeclaredField("price");
                    pf.setAccessible(true);
                    Object pv = pf.get(order);
                    if (pv != null) execPrice = toBigDecimal(pv);
                } catch (Exception ignored) {}
            }

            if (execPrice != null) {
                trade.setExitPrice(execPrice);

                try {
                    BigDecimal entry = trade.getEntryPrice();
                    int qty = trade.getQuantity() == null ? 0 : trade.getQuantity();
                    if (entry != null && qty > 0) {
                        BigDecimal priceDiff = execPrice.subtract(entry);
                        // If original trade was a SELL, reverse pnl sign
                        if (trade.getType() != null && trade.getType().name().equalsIgnoreCase("SELL")) {
                            priceDiff = priceDiff.negate();
                        }
                        trade.setPnl(priceDiff.multiply(new BigDecimal(qty)));
                        updateDailyPnL(trade.getPnl());
                    }
                } catch (Exception e) {
                    log.warn("Could not calculate pnl for trade {}: {}", trade.getId(), e.getMessage());
                }
            }
        }

        tradeRepository.save(trade);
        log.info("Trade (orderId={}) updated to status {}", orderId, mappedStatus);
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Double) return BigDecimal.valueOf((Double) v);
        if (v instanceof Float) return BigDecimal.valueOf((Float) v);
        if (v instanceof Integer) return BigDecimal.valueOf((Integer) v);
        if (v instanceof Long) return BigDecimal.valueOf((Long) v);
        try {
            return new BigDecimal(v.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public void fetchAndUpdateTrades() {
        log.info("Fetching and updating trades from Kite");
        kiteService.fetchAndUpdateTrades();
    }
}