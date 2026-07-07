package com.example.movietickets.exception;

public class InvalidDiscountCodeException extends RuntimeException {

    public InvalidDiscountCodeException(String message) {
        super(message);
    }
}
