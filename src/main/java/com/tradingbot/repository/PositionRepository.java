// java
package com.tradingbot.repository;

import com.tradingbot.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
    Optional<Position> findByTradingSymbol(String tradingSymbol);
}