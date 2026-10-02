package com.storagehub.service;

import com.storagehub.dto.BrowseUnitDto;
import com.storagehub.dto.BrowseUnitsResponse;
import com.storagehub.dto.PricingBreakdownDto;
import com.storagehub.dto.UnitDetailDto;
import com.storagehub.entity.PolicyRule;
import com.storagehub.entity.PolicyRuleType;
import com.storagehub.entity.RentalPolicy;
import com.storagehub.entity.Reservation;
import com.storagehub.entity.ReservationStatus;
import com.storagehub.entity.Unit;
import com.storagehub.entity.UnitStatus;
import com.storagehub.exception.ResourceNotFoundException;
import com.storagehub.repository.PolicyRuleRepository;
import com.storagehub.repository.ReservationRepository;
import com.storagehub.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class UnitService {

    public static final ZoneId ICT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final Set<UnitStatus> EXCLUDED_STATUSES = Set.of(
            UnitStatus.RENTED,
            UnitStatus.MAINTENANCE,
            UnitStatus.RETIRED
    );

    private final UnitRepository unitRepository;
    private final PricingEngine pricingEngine;
    private final PolicyRuleRepository policyRuleRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationExpiryService reservationExpiryService;

    public UnitService(UnitRepository unitRepository,
                       PricingEngine pricingEngine,
                       PolicyRuleRepository policyRuleRepository,
                       ReservationRepository reservationRepository,
                       ReservationExpiryService reservationExpiryService) {
        this.unitRepository = unitRepository;
        this.pricingEngine = pricingEngine;
        this.policyRuleRepository = policyRuleRepository;
        this.reservationRepository = reservationRepository;
        this.reservationExpiryService = reservationExpiryService;
    }

    @Transactional
    public BrowseUnitsResponse browseUnits(String type, String size, LocalDate startDate, Integer durationMonths) {
        reservationExpiryService.expireAllPastReservedReservations();

        LocalDate queryStartDate = (startDate != null) ? startDate : LocalDate.now(ICT_ZONE);
        int duration = (durationMonths != null && durationMonths > 0) ? durationMonths : 1;
        RentalPolicy activePolicy = pricingEngine.resolveActivePolicy(queryStartDate);

        List<Unit> allUnits = unitRepository.findAllWithDetails();
        List<BrowseUnitDto> items = new ArrayList<>();
        int totalUnits = 0;

        for (Unit unit : allUnits) {
            // Exclude RENTED, MAINTENANCE, RETIRED units (AD-4)
            if (EXCLUDED_STATUSES.contains(unit.getStatus())) {
                continue;
            }

            totalUnits++;

            // Check Turnover Buffer & Availability
            int turnoverBufferHours = 0;
            if (unit.getUnitType() != null) {
                turnoverBufferHours = policyRuleRepository.findByPolicy_IdAndUnitType_IdAndRuleType(
                        activePolicy.getId(), unit.getUnitType().getId(), PolicyRuleType.TURNOVER_BUFFER
                ).map(r -> r.getValue().intValue()).orElse(0);
            }

            // Turnover buffer in days
            int turnoverBufferDays = (turnoverBufferHours > 0) ? turnoverBufferHours : 0;

            boolean isInCleaningBuffer = false;
            LocalDate today = LocalDate.now(ICT_ZONE);
            LocalDate availableFromDate = today;

            if (unit.getStatus() == UnitStatus.PREPARING) {
                isInCleaningBuffer = true;
                // Determine buffer cleared date: latest reservation end date + buffer days, or today + buffer days
                List<Reservation> unitReservations = reservationRepository.findByUnitIdOrderByEndDateDesc(unit.getId());
                if (!unitReservations.isEmpty()) {
                    LocalDate latestEnd = unitReservations.get(0).getEndDate();
                    LocalDate candidateDate = latestEnd.plusDays(turnoverBufferDays);
                    availableFromDate = candidateDate.isBefore(today) ? today.plusDays(turnoverBufferDays) : candidateDate;
                } else {
                    availableFromDate = today.plusDays(turnoverBufferDays);
                }
            } else if (unit.getStatus() == UnitStatus.AVAILABLE) {
                availableFromDate = today;
            } else if (unit.getStatus() == UnitStatus.RESERVED) {
                // If reserved, check active reservations
                List<Reservation> activeReservations = reservationRepository.findByUnit_IdAndStatusIn(
                        unit.getId(), List.of(ReservationStatus.PENDING_PAYMENT, ReservationStatus.RESERVED, ReservationStatus.CHECKED_IN)
                );
                if (!activeReservations.isEmpty()) {
                    LocalDate latestEnd = activeReservations.stream()
                            .map(Reservation::getEndDate)
                            .max(LocalDate::compareTo)
                            .orElse(today);
                    LocalDate candidateDate = latestEnd.plusDays(turnoverBufferDays);
                    availableFromDate = candidateDate.isBefore(today) ? today : candidateDate;
                }
            }

            boolean isImmediatelyAvailable = (unit.getStatus() == UnitStatus.AVAILABLE) && !availableFromDate.isAfter(today);

            String availabilityStatus;
            if (isInCleaningBuffer) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
                availabilityStatus = "Available " + availableFromDate.format(formatter) + " · cleaning buffer";
            } else if (isImmediatelyAvailable) {
                availabilityStatus = "Available now";
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
                availabilityStatus = "Available " + availableFromDate.format(formatter);
            }

            // If startDate is requested by user, filter out units not ready by startDate
            if (startDate != null && availableFromDate.isAfter(startDate)) {
                continue;
            }

            // Apply Type Filter
            if (type != null && !type.isBlank() && !type.equalsIgnoreCase("all")) {
                if (unit.getUnitType() == null || !unit.getUnitType().getName().equalsIgnoreCase(type.trim())) {
                    continue;
                }
            }

            // Apply Size Filter
            if (size != null && !size.isBlank() && !size.equalsIgnoreCase("all")) {
                String sizeTrimmed = size.trim();
                boolean matchesSize = false;
                if (unit.getSizeM2() != null) {
                    double m2 = unit.getSizeM2().doubleValue();
                    if (sizeTrimmed.equalsIgnoreCase("Small") && m2 <= 5.0) matchesSize = true;
                    else if (sizeTrimmed.equalsIgnoreCase("Medium") && m2 > 5.0 && m2 <= 10.0) matchesSize = true;
                    else if (sizeTrimmed.equalsIgnoreCase("Large") && m2 > 10.0) matchesSize = true;
                    else if (unit.getSizeM2().toString().equals(sizeTrimmed) || (m2 + " m2").equalsIgnoreCase(sizeTrimmed) || (m2 + " m²").equalsIgnoreCase(sizeTrimmed)) matchesSize = true;
                }
                if (!matchesSize) {
                    continue;
                }
            }

            PricingBreakdownDto pricing = pricingEngine.calculatePricing(unit, duration, queryStartDate);

            String facilityName = (unit.getZone() != null && unit.getZone().getFacility() != null)
                    ? unit.getZone().getFacility().getName() : "Tan Binh Depot";
            String zoneCode = (unit.getZone() != null) ? unit.getZone().getCode() : "A";
            String typeName = (unit.getUnitType() != null) ? unit.getUnitType().getName() : "";
            String typeDesc = (unit.getUnitType() != null) ? unit.getUnitType().getDescription() : null;

            items.add(new BrowseUnitDto(
                    unit.getId(),
                    unit.getCode(),
                    typeName,
                    typeDesc,
                    zoneCode,
                    facilityName,
                    unit.getFloor(),
                    unit.getSizeM2(),
                    unit.getAccessType(),
                    unit.getStatus().name(),
                    "/units/" + unit.getCode() + ".jpg",
                    pricing.monthlyRate(),
                    pricing.depositRate(),
                    availabilityStatus,
                    availableFromDate,
                    isImmediatelyAvailable,
                    isInCleaningBuffer
            ));
        }

        // Sort price low -> high
        items.sort(Comparator.comparing(BrowseUnitDto::monthlyRate));

        return new BrowseUnitsResponse(items, items.size(), totalUnits);
    }

    @Transactional
    public UnitDetailDto getUnitDetail(String code) {
        Unit unit = unitRepository.findByCodeWithDetails(code)
                .or(() -> unitRepository.findByCode(code))
                .orElseThrow(() -> new ResourceNotFoundException("Unit " + code + " not found"));

        reservationExpiryService.expirePastReservationsForUnit(unit.getId());

        PricingBreakdownDto baselinePricing = pricingEngine.calculatePricing(unit, 1, LocalDate.now(ICT_ZONE));

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
