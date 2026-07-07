package com.example.movietickets.unit;

import com.example.movietickets.entity.*;
import com.example.movietickets.repository.SeatTypePriceRepository;
import com.example.movietickets.repository.WeekendPricingRuleRepository;
import com.example.movietickets.service.PricingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private SeatTypePriceRepository seatTypePriceRepository;
    @Mock
    private WeekendPricingRuleRepository weekendPricingRuleRepository;
    @InjectMocks
    private PricingService pricingService;

    private Seat regularSeat(SeatType type) {
        return new Seat(1L, 1L, "A1", type);
    }

    @Test
    void weekdayShowChargesJustBasePrice() {
        when(seatTypePriceRepository.findBySeatType(SeatType.REGULAR))
            .thenReturn(Optional.of(new SeatTypePrice(1L, SeatType.REGULAR, BigDecimal.valueOf(150))));

        // 2024-01-01 is a Monday
        Show show = new Show(1L, 1L, 1L, LocalDateTime.of(2024, 1, 1, 10, 0), LocalDateTime.of(2024, 1, 1, 12, 0));

        BigDecimal price = pricingService.seatPrice(regularSeat(SeatType.REGULAR), show);

        assertThat(price).isEqualByComparingTo("150");
    }

    @Test
    void weekendShowAppliesMultiplierOnTopOfSeatTypeBasePrice() {
        when(seatTypePriceRepository.findBySeatType(SeatType.PREMIUM))
            .thenReturn(Optional.of(new SeatTypePrice(2L, SeatType.PREMIUM, BigDecimal.valueOf(300))));
        when(weekendPricingRuleRepository.findAll())
            .thenReturn(List.of(new WeekendPricingRule(1L, BigDecimal.valueOf(1.20))));

        // 2024-01-06 is a Saturday
        Show show = new Show(1L, 1L, 1L, LocalDateTime.of(2024, 1, 6, 10, 0), LocalDateTime.of(2024, 1, 6, 12, 0));

        BigDecimal price = pricingService.seatPrice(regularSeat(SeatType.PREMIUM), show);

        assertThat(price).isEqualByComparingTo("360.00");
    }
}
