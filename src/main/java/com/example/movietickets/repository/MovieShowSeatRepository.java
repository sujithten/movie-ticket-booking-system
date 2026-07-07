package com.example.movietickets.repository;

import com.example.movietickets.entity.MovieShowSeat;
import com.example.movietickets.entity.MovieShowSeatId;
import com.example.movietickets.entity.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MovieShowSeatRepository extends JpaRepository<MovieShowSeat, MovieShowSeatId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MovieShowSeat m WHERE m.id.showId = :showId AND m.id.seatId = :seatId")
    Optional<MovieShowSeat> findByShowIdAndSeatIdForUpdate(@Param("showId") Long showId, @Param("seatId") Long seatId);

    @Query("SELECT m FROM MovieShowSeat m WHERE m.id.showId = :showId")
    List<MovieShowSeat> findByShowId(@Param("showId") Long showId);

    List<MovieShowSeat> findByStatusAndExpiresAtBefore(SeatStatus status, Instant instant);
}
