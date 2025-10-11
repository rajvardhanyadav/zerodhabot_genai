package com.tradingbot.service;

import com.tradingbot.dto.TradingConfigDto;
import com.tradingbot.strategy.Strategy;
import com.tradingbot.strategy.StrategyType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StrategyServiceImpl implements StrategyService {
    private final Map<StrategyType, Strategy> strategies;

    public StrategyServiceImpl(java.util.List<Strategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(Strategy::getType, s -> s));
    }

    @Override
    public void runStrategy(StrategyType type, TradingConfigDto config) {
        Strategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy found for type: " + type);
        }
        strategy.execute(config);
    }
}
