package com.example.movietickets.repository;

import com.example.movietickets.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Booking> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.status = com.example.movietickets.entity.BookingStatus.CONFIRMED
          AND b.showId IN (SELECT s.id FROM Show s WHERE s.startTime BETWEEN :from AND :to)
        """)
    List<Booking> findConfirmedForShowsStartingBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
