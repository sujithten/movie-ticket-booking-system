package com.example.movietickets;

import com.example.movietickets.config.BookingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(BookingProperties.class)
public class MovieTicketsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MovieTicketsApplication.class, args);
    }
}
