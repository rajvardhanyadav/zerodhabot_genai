package com.tradingbot.controller;

import com.tradingbot.dto.AccountInfo;
import com.tradingbot.dto.TradingConfigDto;
import com.tradingbot.entity.Trade;
import com.tradingbot.repository.TradeRepository;
import com.tradingbot.service.KiteService;
import com.tradingbot.service.StrategyService;
import com.tradingbot.service.TradingService;
import com.tradingbot.strategy.StrategyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
public class DashboardController {

    private static final int UPCOMING_EXPIRY_COUNT = 3;

    private final KiteService kiteService;
    private final TradingService tradingService;
    private final TradeRepository tradeRepository;
    private final StrategyService strategyService;

    public DashboardController(KiteService kiteService, TradingService tradingService, TradeRepository tradeRepository, StrategyService strategyService) {
        this.kiteService = kiteService;
        this.tradingService = tradingService;
        this.tradeRepository = tradeRepository;
        this.strategyService = strategyService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        log.info("Entering dashboard");

        if (!kiteService.isAccessTokenValid()) {
            log.warn("Access token is invalid. Redirecting to login.");
            return "redirect:/";
        }

        populateDashboardModel(model);
        return "dashboard";
    }

    @PostMapping("/start-trading")
    @ResponseBody
    public String startTrading(@RequestBody TradingConfigDto config) {
        log.info("Starting trading with config: {}", config);
        tradingService.startTrading();
        strategyService.runStrategy(StrategyType.BUY_STRADDLE, config);
        return "Trading started";
    }

    @PostMapping("/stop-trading")
    @ResponseBody
    public String stopTrading() {
        log.info("Stopping trading.");
        tradingService.stopTrading();
        return "Trading stopped";
    }

    private void populateDashboardModel(Model model) {
        AccountInfo accountInfo = kiteService.getAccountInfo();
        log.info("Account Info: UserName={}, TotalMargin={}, UserId={}",
                accountInfo.getUserName(), accountInfo.getTotalMargin(), accountInfo.getUserId());

        List<Trade> recentTrades = tradeRepository.findAll();
        List<LocalDate> expiryDates = kiteService.getUpcomingExpiryDates(UPCOMING_EXPIRY_COUNT);
        List<Map<String, Object>> orders = kiteService.fetchOrders();
        Map<String, List<Map<String, Object>>> positions = kiteService.fetchPositions();

        model.addAttribute("orders", orders != null ? orders : List.of());
        model.addAttribute("dayPositions", positions != null ? positions.getOrDefault("day", List.of()) : List.of());
        model.addAttribute("netPositions", positions != null ? positions.getOrDefault("net", List.of()) : List.of());

        model.addAttribute("accountInfo", accountInfo);
        model.addAttribute("recentTrades", recentTrades);
        model.addAttribute("todaysPnL", tradingService.getTodaysPnL());
        model.addAttribute("tradingActive", tradingService.isTradingActive());
        model.addAttribute("expiryDates", expiryDates);
    }
}