package com.tradingbot.service;

import com.tradingbot.dto.TradingConfigDto;
import com.tradingbot.strategy.StrategyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

public interface StrategyService {
    void runStrategy(StrategyType type, TradingConfigDto config);
}
