package com.storagehub.service;

import com.storagehub.dto.PricingBreakdownDto;
import com.storagehub.dto.UnitDetailDto;
import com.storagehub.entity.Unit;
import com.storagehub.exception.ResourceNotFoundException;
import com.storagehub.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class UnitService {

    private final UnitRepository unitRepository;
    private final PricingEngine pricingEngine;

    public UnitService(UnitRepository unitRepository, PricingEngine pricingEngine) {
        this.unitRepository = unitRepository;
        this.pricingEngine = pricingEngine;
    }

    public UnitDetailDto getUnitDetail(String code) {
        Unit unit = unitRepository.findByCodeWithDetails(code)
                .or(() -> unitRepository.findByCode(code))
                .orElseThrow(() -> new ResourceNotFoundException("Unit " + code + " not found"));

        PricingBreakdownDto baselinePricing = pricingEngine.calculatePricing(unit, 1, LocalDate.now());

        List<String> securityFeatures = List.of(
                "24/7 CCTV Monitoring",
                "Personal Access Code (" + unit.getAccessType() + ")",
                "Individually Alarmed Unit",
                "Climate & Humidity Controlled",
                "Fire Protection & Sprinklers"
        );

        String typeDesc = (unit.getUnitType() != null) ? unit.getUnitType().getDescription() : null;
        String facilityName = (unit.getZone() != null && unit.getZone().getFacility() != null)
                ? unit.getZone().getFacility().getName() : "Tan Binh Depot";
        String facilityAddress = (unit.getZone() != null && unit.getZone().getFacility() != null)
                ? unit.getZone().getFacility().getAddress() : "45 Nguyen Van Troi, Tan Binh, Ho Chi Minh City";
        String zoneCode = (unit.getZone() != null) ? unit.getZone().getCode() : "A";

        return new UnitDetailDto(
                unit.getId(),
                unit.getCode(),
                unit.getUnitType().getName(),
                typeDesc,
                facilityName,
                facilityAddress,
                zoneCode,
                unit.getFloor(),
                unit.getSizeM2(),
                unit.getAccessType(),
                unit.getStatus().name(),
                "/units/" + unit.getCode() + ".jpg",
                baselinePricing.monthlyRate(),
                baselinePricing.depositRate(),
                securityFeatures
        );
    }
}
