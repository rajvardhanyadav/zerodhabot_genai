package com.tradingbot.service;

import com.tradingbot.dto.NfoInstrument;
import com.tradingbot.dto.TradingConfigDto;
import com.tradingbot.entity.DailyPnL;
import com.tradingbot.entity.Trade;
import com.tradingbot.repository.DailyPnLRepository;
import com.tradingbot.repository.TradeRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
        return kiteService.placeOrder(symbol, transactionType, 25, "MARKET", price);
    }

    public void saveTrade(String symbol, String transactionType, String orderId, BigDecimal price, String instrumentToken) {
        Trade trade = new Trade(symbol, transactionType, 25, price, STRATEGY_STRADDLE);
        trade.setOrderId(orderId);
        trade.setInstrumentToken(instrumentToken);
        tradeRepository.save(trade);
        log.info("Trade saved: {} at {}", symbol, price);
    }

    public void subscribeToTokens(List<NfoInstrument> symbols) {
        List<Long> tokens = new ArrayList<>();
        tokens.add(Long.valueOf(symbols.get(0).getInstrumentToken()));
        tokens.add(Long.valueOf(symbols.get(1).getInstrumentToken()));
//        kiteTickerService.subscribe(tokens);
        CsvFeedSimulator csvFeedSimulator = new CsvFeedSimulator(kiteTickerService);
        csvFeedSimulator.simulateFeedAsync();
    }

    public void closeTrade(Trade trade, String transactionType, BigDecimal currentPrice, BigDecimal priceDiff) {
        log.info("Placing sell order to close trade for symbol: {}", trade.getSymbol());
        String orderId = kiteService.placeOrder(trade.getSymbol(), TRANSACTION_SELL, trade.getQuantity(), "MARKET", currentPrice);
        if (orderId != null) {
            trade.setStatus(Trade.TradeStatus.CLOSED);
            trade.setExitPrice(currentPrice);
            trade.setExitTimestamp(LocalDateTime.now());
            trade.setPnl(priceDiff.multiply(new BigDecimal(trade.getQuantity())));
            tradeRepository.save(trade);

            updateDailyPnL(trade.getPnl());
            activeTrades = getActivePositions();
            log.info("Active trades remaining: {}", activeTrades.size());
        }
    }

    public List<Trade> getActivePositions() {
        return tradeRepository.findByStatus(STATUS_OPEN);
    }

    public boolean isTradingTime() {
        LocalTime now = LocalTime.now();
        return now.isAfter(LocalTime.of(9, 15)) && now.isBefore(LocalTime.of(15, 30));
    }

    public boolean isTradingStopped() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().plusDays(1).atStartOfDay();
        BigDecimal todaysPnL = Optional.of(tradeRepository.findTodaysPnLBetween(startOfDay, endOfDay))
                .orElse(BigDecimal.ZERO);
        return todaysPnL.compareTo(maxDailyLoss.negate()) <= 0;
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
}