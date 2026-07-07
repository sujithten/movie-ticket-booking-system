package com.example.movietickets.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "booking_seat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingSeat {

    @EmbeddedId
    private BookingSeatId id;

    public BookingSeat(Long bookingId, Long seatId) {
        this.id = new BookingSeatId(bookingId, seatId);
    }
}
