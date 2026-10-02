package com.storagehub.controller;

import com.storagehub.dto.UnitDetailDto;
import com.storagehub.service.UnitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/units")
public class UnitController {

    private final UnitService unitService;

    public UnitController(UnitService unitService) {
        this.unitService = unitService;
    }

    @GetMapping("/{code}")
    public ResponseEntity<UnitDetailDto> getUnitDetail(@PathVariable String code) {
        return ResponseEntity.ok(unitService.getUnitDetail(code));
    }
}
