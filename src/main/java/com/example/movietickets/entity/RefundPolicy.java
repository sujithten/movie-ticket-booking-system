package com.example.movietickets.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "refund_policy")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefundPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cutoff_hours_before_show", nullable = false)
    private Integer cutoffHoursBeforeShow;

    @Column(name = "refund_percentage", nullable = false)
    private Integer refundPercentage;
}
