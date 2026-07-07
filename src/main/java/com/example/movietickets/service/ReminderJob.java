package com.example.movietickets.service;

import com.example.movietickets.config.BookingProperties;
import com.example.movietickets.entity.Booking;
import com.example.movietickets.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Reminder notifications for upcoming shows with confirmed bookings (spec §12). */
@Component
@RequiredArgsConstructor
public class ReminderJob {

    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final BookingProperties bookingProperties;

    @Scheduled(fixedRate = 3_600_000) // hourly
    public void sendUpcomingShowReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowEnd = now.plusHours(bookingProperties.reminderWindowHours());

        for (Booking booking : bookingRepository.findConfirmedForShowsStartingBetween(now, windowEnd)) {
            notificationService.sendShowReminder(booking);
        }
    }
}
