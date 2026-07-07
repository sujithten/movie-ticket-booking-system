package com.example.movietickets.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "seat_type_price")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeatTypePrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, unique = true)
    private SeatType seatType;

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice;
}
