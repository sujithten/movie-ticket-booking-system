package com.example.movietickets.service;

import com.example.movietickets.entity.WeekendPricingRule;
import com.example.movietickets.repository.WeekendPricingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PricingRuleService {

    private final WeekendPricingRuleRepository weekendPricingRuleRepository;

    public WeekendPricingRule getWeekendRule() {
        return weekendPricingRuleRepository.findAll().stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("Weekend pricing rule not seeded"));
    }

    public WeekendPricingRule updateWeekendMultiplier(BigDecimal multiplier) {
        WeekendPricingRule rule = getWeekendRule();
        rule.setMultiplier(multiplier);
        return weekendPricingRuleRepository.save(rule);
    }
}
