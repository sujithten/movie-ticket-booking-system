package com.example.movietickets.service;

import com.example.movietickets.entity.Seat;
import com.example.movietickets.entity.SeatTypePrice;
import com.example.movietickets.entity.Show;
import com.example.movietickets.repository.SeatTypePriceRepository;
import com.example.movietickets.repository.WeekendPricingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;

/**
 * Two independent pricing axes, kept deliberately separate (see spec §10):
 * seat type (quality) and day type (weekend vs. weekday). Neither is persisted
 * per-show — weekend-ness is derived from the show's startTime at read time,
 * so changing the multiplier later never leaves stale data on existing shows.
 */
@Service
@RequiredArgsConstructor
public class PricingService {

    private final SeatTypePriceRepository seatTypePriceRepository;
    private final WeekendPricingRuleRepository weekendPricingRuleRepository;

    public BigDecimal seatPrice(Seat seat, Show show) {
        SeatTypePrice seatTypePrice = seatTypePriceRepository.findBySeatType(seat.getSeatType())
            .orElseThrow(() -> new IllegalStateException("No price configured for seat type " + seat.getSeatType()));

        BigDecimal base = seatTypePrice.getBasePrice();
        if (!isWeekend(show)) {
            return base;
        }

        BigDecimal multiplier = weekendPricingRuleRepository.findAll().stream()
            .findFirst()
            .map(rule -> rule.getMultiplier())
            .orElse(BigDecimal.ONE);
        return base.multiply(multiplier);
    }

    public boolean isWeekend(Show show) {
        DayOfWeek day = show.getStartTime().getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}
