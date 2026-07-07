package com.example.movietickets.repository;

import com.example.movietickets.entity.BookingSeat;
import com.example.movietickets.entity.BookingSeatId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, BookingSeatId> {

    @Query("SELECT bs FROM BookingSeat bs WHERE bs.id.bookingId = :bookingId")
    List<BookingSeat> findByBookingId(@Param("bookingId") Long bookingId);
}
