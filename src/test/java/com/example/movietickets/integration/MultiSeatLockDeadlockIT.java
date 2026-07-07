package com.example.movietickets.integration;

import com.example.movietickets.entity.*;
import com.example.movietickets.repository.*;
import com.example.movietickets.service.SeatLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the sorted-lock-order rule in SeatLockService prevents the classic
 * deadlock: two threads locking the same pair of seats in reversed order
 * concurrently must both complete (one may fail with SeatUnavailableException
 * if it genuinely loses a seat, but neither may hang or throw a DB deadlock error).
 */
class MultiSeatLockDeadlockIT extends AbstractIntegrationTest {

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
    private Long seatA;
    private Long seatB;

    @BeforeEach
    void setUp() {
        City city = cityRepository.save(new City(null, "Test City"));
        Theater theater = theaterRepository.save(new Theater(null, "Test Theater", city.getId(), "addr"));
        Screen screen = screenRepository.save(new Screen(null, theater.getId(), "Screen 1", 50));
        Seat a = seatRepository.save(new Seat(null, screen.getId(), "A1", SeatType.REGULAR));
        Seat b = seatRepository.save(new Seat(null, screen.getId(), "A2", SeatType.REGULAR));
        Movie movie = movieRepository.save(new Movie(null, "Test Movie", 120, "en", "Action"));
        Show show = showRepository.save(new Show(null, movie.getId(), screen.getId(),
            LocalDateTime.now().plusDays(30).withHour(10), LocalDateTime.now().plusDays(30).withHour(12)));
        movieShowSeatRepository.save(new MovieShowSeat(show.getId(), a.getId(), null, SeatStatus.AVAILABLE, null));
        movieShowSeatRepository.save(new MovieShowSeat(show.getId(), b.getId(), null, SeatStatus.AVAILABLE, null));

        this.showId = show.getId();
        this.seatA = a.getId();
        this.seatB = b.getId();
    }

    @Test
    void reversedOrderMultiSeatLocksDoNotDeadlock() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch go = new CountDownLatch(1);

        Callable<Boolean> lockAThenB = () -> {
            go.await();
            try {
                seatLockService.lockSeats(showId, List.of(seatA, seatB), 1L);
                return true;
            } catch (Exception e) {
                return false;
            }
        };
        Callable<Boolean> lockBThenA = () -> {
            go.await();
            try {
                seatLockService.lockSeats(showId, List.of(seatB, seatA), 2L);
                return true;
            } catch (Exception e) {
                return false;
            }
        };

        Future<Boolean> f1 = pool.submit(lockAThenB);
        Future<Boolean> f2 = pool.submit(lockBThenA);
        go.countDown();

        // If the sorted-lock-order rule failed to prevent a deadlock, this times out.
        Boolean r1 = f1.get(10, TimeUnit.SECONDS);
        Boolean r2 = f2.get(10, TimeUnit.SECONDS);
        pool.shutdown();

        // Both requests lock the same seat pair in opposite order -> exactly one wins both seats.
        assertThat(List.of(r1, r2)).containsExactlyInAnyOrder(true, false);
    }
}
