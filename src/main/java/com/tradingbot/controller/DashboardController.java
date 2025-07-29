package com.tradingbot.controller;

import com.tradingbot.dto.AccountInfo;
import com.tradingbot.entity.Trade;
import com.tradingbot.repository.TradeRepository;
import com.tradingbot.service.KiteService;
import com.tradingbot.service.TradingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Slf4j
@Controller
public class DashboardController {

    @Autowired
    private KiteService kiteService;

    @Autowired
    private TradingService tradingService;

    @Autowired
    private TradeRepository tradeRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        log.info("Entering dashboard");
        if (!kiteService.isAccessTokenValid()) {
            return "redirect:/";
        }

        AccountInfo accountInfo = kiteService.getAccountInfo();
        log.info(accountInfo.getUserName());
        log.info(String.valueOf(accountInfo.getTotalMargin()));
        log.info(accountInfo.getUserId());
        List<Trade> recentTrades = tradeRepository.findAll();

        model.addAttribute("accountInfo", accountInfo);
        model.addAttribute("recentTrades", recentTrades);
        model.addAttribute("todaysPnL", tradingService.getTodaysPnL());
        model.addAttribute("tradingActive", tradingService.isTradingActive());

        return "dashboard";
    }

    @PostMapping("/start-trading")
    @ResponseBody
    public String startTrading() {
        tradingService.startTrading();
        return "Trading started";
    }

    @PostMapping("/stop-trading")
    @ResponseBody
    public String stopTrading() {
        tradingService.stopTrading();
        return "Trading stopped";
    }
}