package com.example.movietickets.unit;

import com.example.movietickets.config.BookingProperties;
import com.example.movietickets.entity.MovieShowSeat;
import com.example.movietickets.entity.SeatStatus;
import com.example.movietickets.exception.SeatUnavailableException;
import com.example.movietickets.repository.MovieShowSeatRepository;
import com.example.movietickets.service.SeatLockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Seat-lock availability/expiry predicate in isolation (spec §14), mocked repo — no DB. */
@ExtendWith(MockitoExtension.class)
class SeatLockServiceTest {

    @Mock
    private MovieShowSeatRepository movieShowSeatRepository;

    @Test
    void lockingAnAvailableSeatSucceeds() {
        MovieShowSeat row = new MovieShowSeat(1L, 1L, null, SeatStatus.AVAILABLE, null);
        when(movieShowSeatRepository.findByShowIdAndSeatIdForUpdate(1L, 1L)).thenReturn(Optional.of(row));
        when(movieShowSeatRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SeatLockService svc = new SeatLockService(movieShowSeatRepository, new BookingProperties(10, 1, 24));
        MovieShowSeat locked = svc.lockSeat(1L, 1L, 42L);

        assertThat(locked.getStatus()).isEqualTo(SeatStatus.LOCKED);
        assertThat(locked.getUserId()).isEqualTo(42L);
    }

    @Test
    void lockingAnUnexpiredLockedSeatFails() {
        MovieShowSeat row = new MovieShowSeat(1L, 1L, 99L, SeatStatus.LOCKED, Instant.now().plusSeconds(600));
        when(movieShowSeatRepository.findByShowIdAndSeatIdForUpdate(1L, 1L)).thenReturn(Optional.of(row));

        SeatLockService svc = new SeatLockService(movieShowSeatRepository, new BookingProperties(10, 1, 24));

        assertThatThrownBy(() -> svc.lockSeat(1L, 1L, 42L)).isInstanceOf(SeatUnavailableException.class);
    }

    @Test
    void lockingAnExpiredLockedSeatReclaimsIt() {
        MovieShowSeat row = new MovieShowSeat(1L, 1L, 99L, SeatStatus.LOCKED, Instant.now().minusSeconds(60));
        when(movieShowSeatRepository.findByShowIdAndSeatIdForUpdate(1L, 1L)).thenReturn(Optional.of(row));
        when(movieShowSeatRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SeatLockService svc = new SeatLockService(movieShowSeatRepository, new BookingProperties(10, 1, 24));
        MovieShowSeat locked = svc.lockSeat(1L, 1L, 42L);

        assertThat(locked.getStatus()).isEqualTo(SeatStatus.LOCKED);
        assertThat(locked.getUserId()).isEqualTo(42L);
    }
}
