package com.example.movietickets.payment;

public record PaymentResult(boolean success, String reference) {

    public static PaymentResult success(String reference) {
        return new PaymentResult(true, reference);
    }

    public static PaymentResult declined() {
        return new PaymentResult(false, null);
    }
}
