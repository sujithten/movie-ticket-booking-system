package com.example.movietickets.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiError.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(SeatUnavailableException.class)
    public ResponseEntity<ApiError> handleSeatUnavailable(SeatUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiError.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ApiError> handlePaymentFailed(PaymentFailedException ex) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
            .body(ApiError.of(402, "Payment Required", ex.getMessage()));
    }

    @ExceptionHandler(InvalidBookingStateException.class)
    public ResponseEntity<ApiError> handleInvalidState(InvalidBookingStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiError.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(InvalidDiscountCodeException.class)
    public ResponseEntity<ApiError> handleInvalidDiscount(InvalidDiscountCodeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiError.of(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(ScheduleConflictException.class)
    public ResponseEntity<ApiError> handleScheduleConflict(ScheduleConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiError.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiError.of(400, "Bad Request", "Validation failed", details));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiError.of(403, "Forbidden", "You do not have permission to perform this action"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiError.of(400, "Bad Request", ex.getMessage()));
    }
}
