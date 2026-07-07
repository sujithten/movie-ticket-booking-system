package com.example.movietickets.unit;

import com.example.movietickets.dto.request.ShowRequest;
import com.example.movietickets.entity.Show;
import com.example.movietickets.exception.ScheduleConflictException;
import com.example.movietickets.repository.MovieShowSeatRepository;
import com.example.movietickets.repository.SeatRepository;
import com.example.movietickets.repository.ShowRepository;
import com.example.movietickets.service.ShowService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** Show scheduling-conflict check in isolation (spec §14). */
@ExtendWith(MockitoExtension.class)
class ShowServiceTest {

    @Mock
    private ShowRepository showRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private MovieShowSeatRepository movieShowSeatRepository;
    @InjectMocks
    private ShowService showService;

    @Test
    void overlappingShowOnSameScreenIsRejected() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 1, 12, 0);
        ShowRequest request = new ShowRequest(1L, 1L, start, end);

        when(showRepository.findOverlapping(eq(1L), any(), any()))
            .thenReturn(List.of(new Show(99L, 5L, 1L, start.minusHours(1), end.minusHours(1))));

        assertThatThrownBy(() -> showService.createShow(request))
            .isInstanceOf(ScheduleConflictException.class);
    }
}
