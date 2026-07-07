package com.example.movietickets.repository;

import com.example.movietickets.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {

    @Query("""
        SELECT s FROM Show s
        WHERE s.screenId = :screenId
          AND s.startTime < :endTime
          AND s.endTime > :startTime
        """)
    List<Show> findOverlapping(@Param("screenId") Long screenId,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);

    List<Show> findByScreenIdIn(List<Long> screenIds);

    @Query("SELECT DISTINCT s.movieId FROM Show s WHERE s.screenId IN :screenIds")
    List<Long> findDistinctMovieIdsByScreenIdIn(@Param("screenIds") List<Long> screenIds);
}
