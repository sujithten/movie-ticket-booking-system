package com.example.movietickets.integration;

import com.example.movietickets.entity.*;
import com.example.movietickets.repository.*;
import com.example.movietickets.service.SeatLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Spec §14: a lock attempt on an already-expired LOCKED row must reclaim it directly, not via the sweeper. */
class ExpiredLockReclaimIT extends AbstractIntegrationTest {

    @Autowired
    private SeatLockService seatLockService;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private TheaterRepository theaterRepository;
    @Autowired
    private ScreenRepository screenRepository;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private ShowRepository showRepository;
    @Autowired
    private MovieShowSeatRepository movieShowSeatRepository;

    private Long showId;
    private Long seatId;

    @BeforeEach
    void setUp() {
        City city = cityRepository.save(new City(null, "Test City"));
        Theater theater = theaterRepository.save(new Theater(null, "Test Theater", city.getId(), "addr"));
        Screen screen = screenRepository.save(new Screen(null, theater.getId(), "Screen 1", 50));
        Seat seat = seatRepository.save(new Seat(null, screen.getId(), "A1", SeatType.REGULAR));
        Movie movie = movieRepository.save(new Movie(null, "Test Movie", 120, "en", "Action"));
        Show show = showRepository.save(new Show(null, movie.getId(), screen.getId(),
            LocalDateTime.now().plusDays(30).withHour(10), LocalDateTime.now().plusDays(30).withHour(12)));

        // Simulate a stale lock: LOCKED with expiresAt already in the past, held by a different user.
        movieShowSeatRepository.save(new MovieShowSeat(show.getId(), seat.getId(), 999L,
            SeatStatus.LOCKED, Instant.now().minusSeconds(60)));

        this.showId = show.getId();
        this.seatId = seat.getId();
    }

    @Test
    void newLockAttemptReclaimsExpiredLockDirectly() {
        MovieShowSeat locked = seatLockService.lockSeat(showId, seatId, 42L);

        assertThat(locked.getStatus()).isEqualTo(SeatStatus.LOCKED);
        assertThat(locked.getUserId()).isEqualTo(42L);
        assertThat(locked.getExpiresAt()).isAfter(Instant.now());
    }
}
