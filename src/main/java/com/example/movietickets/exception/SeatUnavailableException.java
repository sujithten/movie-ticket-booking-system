package com.example.movietickets.exception;

public class SeatUnavailableException extends RuntimeException {

    public SeatUnavailableException(Long seatId) {
        super("Seat " + seatId + " is not available");
    }
}
