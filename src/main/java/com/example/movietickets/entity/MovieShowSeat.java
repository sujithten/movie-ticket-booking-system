package com.example.movietickets.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * The seat map for a given show. One row per (show, seat), provisioned at
 * show-creation time and updated in place through AVAILABLE -> LOCKED -> CONFIRMED.
 */
@Entity
@Table(name = "movie_show_seat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovieShowSeat {

    @EmbeddedId
    private MovieShowSeatId id;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public MovieShowSeat(Long showId, Long seatId, Long userId, SeatStatus status, Instant expiresAt) {
        this.id = new MovieShowSeatId(showId, seatId);
        this.userId = userId;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    @Transient
    public Long getShowId() {
        return id.getShowId();
    }

    @Transient
    public Long getSeatId() {
        return id.getSeatId();
    }
}
