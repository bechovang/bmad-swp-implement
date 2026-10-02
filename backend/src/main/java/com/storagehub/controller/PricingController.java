package com.storagehub.controller;

import com.storagehub.dto.PricingBreakdownDto;
import com.storagehub.service.PricingEngine;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/pricing")
@Validated
public class PricingController {

    private final PricingEngine pricingEngine;

    public PricingController(PricingEngine pricingEngine) {
        this.pricingEngine = pricingEngine;
    }

    @GetMapping("/calculate")
    public ResponseEntity<PricingBreakdownDto> calculatePricing(
            @RequestParam String unitCode,
            @RequestParam @Min(value = 1, message = "Duration must be at least 1 month")
            @Max(value = 120, message = "Duration cannot exceed 120 months") int durationMonths,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        return ResponseEntity.ok(pricingEngine.calculatePricing(unitCode, durationMonths, startDate));
    }
}
