package com.tradingbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Utility service for trading-related operations such as market hours, holidays, and formatting.
 */
@Service
@Slf4j
public class TradingUtilityService {

    private static final List<LocalDate> MARKET_HOLIDAYS = Arrays.asList(
            LocalDate.of(2024, 1, 26), // Republic Day
            LocalDate.of(2024, 3, 8),  // Holi
            LocalDate.of(2024, 8, 15), // Independence Day
            LocalDate.of(2024, 10, 2), // Gandhi Jayanti
            LocalDate.of(2024, 11, 1)  // Diwali
    );

    private static final LocalTime MARKET_OPEN = LocalTime.of(0, 1);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(23, 59);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Checks if the market is open at the current time.
     *
     * @return true if the market is open, false otherwise.
     */
    public boolean isMarketOpen() {
        return isMarketOpen(LocalDateTime.now());
    }

    /**
     * Checks if the market is open at a given date and time.
     *
     * @param dateTime The date and time to check.
     * @return true if the market is open, false otherwise.
     */
    public boolean isMarketOpen(LocalDateTime dateTime) {
        LocalDate date = dateTime.toLocalDate();
        LocalTime time = dateTime.toLocalTime();

        return !isWeekend(date) && !isHoliday(date) && isWithinTradingHours(time);
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private boolean isHoliday(LocalDate date) {
        return MARKET_HOLIDAYS.contains(date);
    }

    private boolean isWithinTradingHours(LocalTime time) {
        return !time.isBefore(MARKET_OPEN) && !time.isAfter(MARKET_CLOSE);
    }

    /**
     * Gets the expiry date for the current or next week.
     *
     * @return The expiry date as a formatted string.
     */
    public String getNextExpiryDate() {
        LocalDate today = LocalDate.now();
        LocalDate thursday = today.with(DayOfWeek.THURSDAY);

        if (today.isAfter(thursday)) {
            thursday = thursday.plusWeeks(1);
        }

        return thursday.format(DATE_FORMATTER);
    }

    /**
     * Rounds a price to the nearest strike interval.
     *
     * @param price          The price to round.
     * @param strikeInterval The strike interval to round to.
     * @return The rounded price.
     */
    public BigDecimal roundToNearestStrike(BigDecimal price, int strikeInterval) {
        return price.divide(BigDecimal.valueOf(strikeInterval), 0, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(strikeInterval));
    }

    /**
     * Formats a profit or loss value as a currency string.
     *
     * @param pnl The profit or loss value.
     * @return The formatted string.
     */
    public String formatPnL(BigDecimal pnl) {
        return String.format("₹%,.2f", pnl);
    }

    /**
     * Formats a percentage value as a string.
     *
     * @param percentage The percentage value.
     * @return The formatted string.
     */
    public String formatPercentage(BigDecimal percentage) {
        return String.format("%.2f%%", percentage);
    }
}