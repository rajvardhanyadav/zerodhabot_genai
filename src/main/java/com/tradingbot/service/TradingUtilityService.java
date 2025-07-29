package com.tradingbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class TradingUtilityService {

    private static final List<LocalDate> MARKET_HOLIDAYS = Arrays.asList(
            // Add market holidays for current year
            LocalDate.of(2024, 1, 26), // Republic Day
            LocalDate.of(2024, 3, 8),  // Holi
            LocalDate.of(2024, 8, 15), // Independence Day
            LocalDate.of(2024, 10, 2), // Gandhi Jayanti
            LocalDate.of(2024, 11, 1)  // Diwali
            // Add more holidays as needed
    );

    public boolean isMarketOpen() {
        return isMarketOpen(LocalDateTime.now());
    }

    public boolean isMarketOpen(LocalDateTime dateTime) {
        LocalDate date = dateTime.toLocalDate();
        LocalTime time = dateTime.toLocalTime();

        // Check if it's a weekend
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }

        // Check if it's a holiday
        if (MARKET_HOLIDAYS.contains(date)) {
            return false;
        }

        // Check trading hours (9:15 AM to 3:30 PM)
        LocalTime marketOpen = LocalTime.of(9, 15);
        LocalTime marketClose = LocalTime.of(15, 30);

        return time.isAfter(marketOpen) && time.isBefore(marketClose);
    }

    public String getCurrentWeekExpiry() {
        LocalDate today = LocalDate.now();
        LocalDate thursday = today.with(DayOfWeek.THURSDAY);

        // If today is after Thursday, get next week's Thursday
        if (today.isAfter(thursday)) {
            thursday = thursday.plusWeeks(1);
        }

        return thursday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    public double roundToNearestStrike(double price, int strikeInterval) {
        return Math.round(price / strikeInterval) * strikeInterval;
    }

    public String formatPnL(double pnl) {
        return String.format("₹%.2f", pnl);
    }

    public String formatPercentage(double percentage) {
        return String.format("%.2f%%", percentage);
    }
}
