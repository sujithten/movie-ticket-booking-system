package com.example.movietickets.repository;

import com.example.movietickets.entity.WeekendPricingRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeekendPricingRuleRepository extends JpaRepository<WeekendPricingRule, Long> {
}
