package com.tradingbot.service;

import com.tradingbot.service.KiteTickerService;
import com.zerodhatech.models.Tick;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CsvFeedSimulator {
    private final KiteTickerService kiteTickerService;

    public CsvFeedSimulator(KiteTickerService kiteTickerService) {
        this.kiteTickerService=kiteTickerService;
    }

    public void simulateFeedAsync() {
        new Thread(this::simulateFeed).start();
    }

    /**
     * Simulates WebSocket data feed by reading two CSV files and processing ticks.
     */
    public void simulateFeed() {
        try {
            List<Tick> ticksFile1 = readCsvFile("banknifty_tick_data_dummy_12806402.csv");
            List<Tick> ticksFile2 = readCsvFile("banknifty_tick_data_dummy_12806658.csv");

            int minSize = Math.min(ticksFile1.size(), ticksFile2.size());
            for (int i = 0; i < 5; i++) {
                List<Tick> tickPair = List.of(ticksFile1.get(i), ticksFile2.get(i));
                kiteTickerService.processTicks(tickPair);
            }
        } catch (Exception e) {
            log.error("Error simulating feed: {}", e.getMessage(), e);
        }
    }

    private List<Tick> readCsvFile(String fileName) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(fileName))))) {

            return reader.lines()
                    .skip(1) // Skip header
                    .map(this::mapToTick)
                    .collect(Collectors.toList());
        }
    }

    private Tick mapToTick(String line) {
        String[] fields = line.split(",");
        Tick tick = new Tick();
        tick.setInstrumentToken(Long.parseLong(fields[1].trim())); // Assuming first column is instrument token
        tick.setLastTradedPrice(Double.parseDouble(fields[2].trim())); // Assuming second column is price
        return tick;
    }
}