package com.example.movietickets.service;

import com.example.movietickets.config.BookingProperties;
import com.example.movietickets.entity.MovieShowSeat;
import com.example.movietickets.entity.SeatStatus;
import com.example.movietickets.exception.SeatUnavailableException;
import com.example.movietickets.repository.MovieShowSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Seat locking — the core of the concurrency story (spec §5).
 *
 * movie_show_seat has exactly one row per (show_id, seat_id), created once at
 * show-creation time. Locking a seat means taking a `SELECT ... FOR UPDATE` on
 * that single row, which means only one transaction can be evaluating/updating
 * a given (showId, seatId) at a time — competing requests queue behind the row
 * lock rather than racing on an insert.
 */
@Service
@RequiredArgsConstructor
public class SeatLockService {

    private final MovieShowSeatRepository movieShowSeatRepository;
    private final BookingProperties bookingProperties;

    @Transactional
    public MovieShowSeat lockSeat(Long showId, Long seatId, Long userId) {
        return lockOne(showId, seatId, userId);
    }

    /**
     * Locks multiple seats for one show in a single transaction.
     *
     * Seat IDs are sorted before locking so that two concurrent multi-seat
     * requests that share seats always acquire FOR UPDATE locks in the same
     * order — this is what prevents a classic deadlock where request A locks
     * {5, 7} while request B locks {7, 5} at the same time. All-or-nothing
     * falls out of @Transactional for free: any SeatUnavailableException here
     * rolls back the whole transaction, releasing every lock taken so far.
     */
    @Transactional
    public List<MovieShowSeat> lockSeats(Long showId, List<Long> seatIds, Long userId) {
        List<Long> orderedSeatIds = seatIds.stream().sorted().toList();

        List<MovieShowSeat> locked = new ArrayList<>();
        for (Long seatId : orderedSeatIds) {
            locked.add(lockOne(showId, seatId, userId));
        }
        return locked;
    }

    private MovieShowSeat lockOne(Long showId, Long seatId, Long userId) {
        MovieShowSeat row = movieShowSeatRepository.findByShowIdAndSeatIdForUpdate(showId, seatId)
            .orElseThrow(() -> new SeatUnavailableException(seatId));

        boolean available = row.getStatus() == SeatStatus.AVAILABLE
            || (row.getStatus() == SeatStatus.LOCKED && row.getExpiresAt() != null
                && row.getExpiresAt().isBefore(Instant.now()));

        if (!available) {
            throw new SeatUnavailableException(seatId);
        }

        row.setStatus(SeatStatus.LOCKED);
        row.setUserId(userId);
        row.setExpiresAt(Instant.now().plusSeconds(bookingProperties.lockDurationMinutes() * 60L));
        return movieShowSeatRepository.save(row);
    }

    @Transactional
    public void releaseExpiredLocks() {
        List<MovieShowSeat> expired = movieShowSeatRepository
            .findByStatusAndExpiresAtBefore(SeatStatus.LOCKED, Instant.now());
        expired.forEach(s -> {
            s.setStatus(SeatStatus.AVAILABLE);
            s.setUserId(null);
            s.setExpiresAt(null);
        });
        movieShowSeatRepository.saveAll(expired);
    }
}
