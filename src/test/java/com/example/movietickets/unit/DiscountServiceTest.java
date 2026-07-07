package com.example.movietickets.unit;

import com.example.movietickets.entity.DiscountCode;
import com.example.movietickets.entity.DiscountType;
import com.example.movietickets.exception.InvalidDiscountCodeException;
import com.example.movietickets.repository.DiscountCodeRepository;
import com.example.movietickets.service.DiscountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {

    @Mock
    private DiscountCodeRepository discountCodeRepository;
    @InjectMocks
    private DiscountService discountService;

    @Test
    void noCodeReturnsOrderAmountUnchanged() {
        BigDecimal result = discountService.applyDiscount(null, BigDecimal.valueOf(500));
        assertThat(result).isEqualByComparingTo("500");
    }

    @Test
    void unknownCodeThrows() {
        when(discountCodeRepository.findByCode("BADCODE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> discountService.applyDiscount("BADCODE", BigDecimal.valueOf(500)))
            .isInstanceOf(InvalidDiscountCodeException.class);
    }

    @Test
    void expiredCodeThrows() {
        DiscountCode code = new DiscountCode(1L, "OLD10", DiscountType.FLAT, BigDecimal.TEN, null,
            null, LocalDateTime.now().minusDays(1));
        when(discountCodeRepository.findByCode("OLD10")).thenReturn(Optional.of(code));

        assertThatThrownBy(() -> discountService.applyDiscount("OLD10", BigDecimal.valueOf(500)))
            .isInstanceOf(InvalidDiscountCodeException.class);
    }

    @Test
    void belowMinimumOrderAmountThrows() {
        DiscountCode code = new DiscountCode(1L, "MIN100", DiscountType.FLAT, BigDecimal.TEN,
            BigDecimal.valueOf(100), null, null);
        when(discountCodeRepository.findByCode("MIN100")).thenReturn(Optional.of(code));

        assertThatThrownBy(() -> discountService.applyDiscount("MIN100", BigDecimal.valueOf(50)))
            .isInstanceOf(InvalidDiscountCodeException.class);
    }

    @Test
    void flatDiscountSubtractsValue() {
        DiscountCode code = new DiscountCode(1L, "FLAT20", DiscountType.FLAT, BigDecimal.valueOf(20),
            null, null, null);
        when(discountCodeRepository.findByCode("FLAT20")).thenReturn(Optional.of(code));

        BigDecimal result = discountService.applyDiscount("FLAT20", BigDecimal.valueOf(100));

        assertThat(result).isEqualByComparingTo("80");
    }

    @Test
    void percentageDiscountAppliesCorrectMath() {
        DiscountCode code = new DiscountCode(1L, "PCT10", DiscountType.PERCENTAGE, BigDecimal.valueOf(10),
            null, null, null);
        when(discountCodeRepository.findByCode("PCT10")).thenReturn(Optional.of(code));

        BigDecimal result = discountService.applyDiscount("PCT10", BigDecimal.valueOf(200));

        assertThat(result).isEqualByComparingTo("180");
    }
}
