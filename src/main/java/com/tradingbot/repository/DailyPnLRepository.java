package com.tradingbot.repository;

import com.tradingbot.entity.DailyPnL;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyPnLRepository extends JpaRepository<DailyPnL, Long> {
    Optional<DailyPnL> findByDate(LocalDate date);
}
