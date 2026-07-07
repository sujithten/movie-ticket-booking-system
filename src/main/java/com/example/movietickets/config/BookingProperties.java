package com.example.movietickets.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "booking")
public record BookingProperties(
    int lockDurationMinutes,
    int cancellationMinHoursBeforeShow,
    int reminderWindowHours
) {
}
