package com.example.movietickets.controller;

import com.example.movietickets.dto.request.WeekendMultiplierRequest;
import com.example.movietickets.entity.WeekendPricingRule;
import com.example.movietickets.service.PricingRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pricing-rules")
@RequiredArgsConstructor
public class PricingRuleController {

    private final PricingRuleService pricingRuleService;

    @GetMapping("/weekend")
    public WeekendPricingRule get() {
        return pricingRuleService.getWeekendRule();
    }

    @PatchMapping("/weekend")
    @PreAuthorize("hasRole('ADMIN')")
    public WeekendPricingRule updateWeekendMultiplier(@Valid @RequestBody WeekendMultiplierRequest request) {
        return pricingRuleService.updateWeekendMultiplier(request.multiplier());
    }
}
