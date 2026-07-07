package com.example.movietickets.integration;

import com.example.movietickets.entity.*;
import com.example.movietickets.exception.SeatUnavailableException;
import com.example.movietickets.repository.*;
import com.example.movietickets.service.SeatLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The highest-priority test in the spec (§14): proves the row-locking design
 * actually serializes concurrent attempts on the same seat with zero double-allocation.
 */
class SeatLockConcurrencyIT extends AbstractIntegrationTest {

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
        // pick a weekday far in the future to keep the show deterministic across test runs
        Show show = showRepository.save(new Show(null, movie.getId(), screen.getId(),
            LocalDateTime.now().plusDays(30).withHour(10), LocalDateTime.now().plusDays(30).withHour(12)));
        movieShowSeatRepository.save(new MovieShowSeat(show.getId(), seat.getId(), null, SeatStatus.AVAILABLE, null));

        this.showId = show.getId();
        this.seatId = seat.getId();
    }

    @Test
    void exactlyOneThreadWinsTheSameSeat() throws InterruptedException {
        int threadCount = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        List<Future<?>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            long userId = i + 1L;
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    seatLockService.lockSeat(showId, seatId, userId);
                    successes.incrementAndGet();
                } catch (SeatUnavailableException e) {
                    failures.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        ready.await();
        go.countDown();
        for (Future<?> f : futures) {
            try {
                f.get(10, TimeUnit.SECONDS);
            } catch (ExecutionException | TimeoutException e) {
                throw new RuntimeException(e);
            }
        }
        pool.shutdown();

        assertThat(successes.get()).isEqualTo(1);
        assertThat(failures.get()).isEqualTo(threadCount - 1);

        MovieShowSeat row = movieShowSeatRepository.findByShowId(showId).get(0);
        assertThat(row.getStatus()).isEqualTo(SeatStatus.LOCKED);
    }
}
