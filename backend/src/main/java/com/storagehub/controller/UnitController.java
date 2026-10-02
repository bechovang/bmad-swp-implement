package com.storagehub.controller;

import com.storagehub.dto.BrowseUnitsResponse;
import com.storagehub.dto.UnitDetailDto;
import com.storagehub.service.UnitService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/units")
public class UnitController {

    private final UnitService unitService;

    public UnitController(UnitService unitService) {
        this.unitService = unitService;
    }

    @GetMapping("/browse")
    public ResponseEntity<BrowseUnitsResponse> browseUnits(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false, defaultValue = "1") Integer durationMonths
    ) {
        return ResponseEntity.ok(unitService.browseUnits(type, size, startDate, durationMonths));
    }

    @GetMapping("/{code}")
    public ResponseEntity<UnitDetailDto> getUnitDetail(@PathVariable String code) {
        return ResponseEntity.ok(unitService.getUnitDetail(code));
    }
}
