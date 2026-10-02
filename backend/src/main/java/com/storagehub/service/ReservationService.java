package com.storagehub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storagehub.dto.CreateReservationRequest;
import com.storagehub.dto.PricingBreakdownDto;
import com.storagehub.dto.ReservationDto;
import com.storagehub.entity.Action;
import com.storagehub.entity.Contract;
import com.storagehub.entity.ContractStatus;
import com.storagehub.entity.EntityType;
import com.storagehub.entity.PolicyRuleType;
import com.storagehub.entity.RentalPolicy;
import com.storagehub.entity.Reservation;
import com.storagehub.entity.ReservationStatus;
import com.storagehub.entity.RoleName;
import com.storagehub.entity.Unit;
import com.storagehub.entity.UnitStatus;
import com.storagehub.entity.User;
import com.storagehub.exception.BusinessRuleException;
import com.storagehub.exception.ResourceNotFoundException;
import com.storagehub.repository.ContractRepository;
import com.storagehub.repository.PolicyRuleRepository;
import com.storagehub.repository.ReservationRepository;
import com.storagehub.repository.UnitRepository;
import com.storagehub.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class ReservationService {

    private static final Set<UnitStatus> BLOCKED_UNIT_STATUSES = Set.of(
            UnitStatus.RENTED,
            UnitStatus.MAINTENANCE,
            UnitStatus.RETIRED
    );

    private static final List<ReservationStatus> ACTIVE_RESERVATION_STATUSES = List.of(
            ReservationStatus.PENDING_PAYMENT,
            ReservationStatus.RESERVED,
            ReservationStatus.CHECKED_IN
    );

    private final ReservationRepository reservationRepository;
    private final UnitRepository unitRepository;
    private final UserRepository userRepository;
    private final ContractRepository contractRepository;
    private final PolicyRuleRepository policyRuleRepository;
    private final PricingEngine pricingEngine;
    private final LogService logService;
    private final ObjectMapper objectMapper;

    public ReservationService(ReservationRepository reservationRepository,
                              UnitRepository unitRepository,
                              UserRepository userRepository,
                              ContractRepository contractRepository,
                              PolicyRuleRepository policyRuleRepository,
                              PricingEngine pricingEngine,
                              LogService logService,
                              ObjectMapper objectMapper) {
        this.reservationRepository = reservationRepository;
        this.unitRepository = unitRepository;
        this.userRepository = userRepository;
        this.contractRepository = contractRepository;
        this.policyRuleRepository = policyRuleRepository;
        this.pricingEngine = pricingEngine;
        this.logService = logService;
        this.objectMapper = objectMapper;
    }

    /**
     * Atomically validates unit availability and creates a PENDING_PAYMENT reservation
     * with locked price snapshot (AD-11, FR-5).
     */
    public ReservationDto createReservation(CreateReservationRequest request, Long customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        Unit unit = unitRepository.findByCodeWithDetails(request.unitCode())
                .or(() -> unitRepository.findByCode(request.unitCode()))
                .orElseThrow(() -> new ResourceNotFoundException("Unit " + request.unitCode() + " not found"));

        // 1. Check unit basic status
        if (BLOCKED_UNIT_STATUSES.contains(unit.getStatus())) {
            throw new BusinessRuleException("UNIT_UNAVAILABLE",
                    "Unit " + unit.getCode() + " is currently " + unit.getStatus().name().toLowerCase() + " and cannot be reserved");
        }

        LocalDate startDate = request.startDate();
        LocalDate endDate = startDate.plusMonths(request.durationMonths());
        RentalPolicy activePolicy = pricingEngine.resolveActivePolicy(startDate);

        // 2. Check turnover buffer for PREPARING units
        if (unit.getStatus() == UnitStatus.PREPARING) {
            int turnoverBufferHours = 0;
            if (unit.getUnitType() != null) {
                turnoverBufferHours = policyRuleRepository.findByPolicy_IdAndUnitType_IdAndRuleType(
                        activePolicy.getId(), unit.getUnitType().getId(), PolicyRuleType.TURNOVER_BUFFER
                ).map(r -> r.getValue().intValue()).orElse(0);
            }
            int turnoverBufferDays = turnoverBufferHours;
            List<Reservation> pastReservations = reservationRepository.findByUnitIdOrderByEndDateDesc(unit.getId());
            LocalDate availableFromDate = LocalDate.now().plusDays(turnoverBufferDays);
            if (!pastReservations.isEmpty()) {
                LocalDate latestEnd = pastReservations.get(0).getEndDate();
                LocalDate cand = latestEnd.plusDays(turnoverBufferDays);
                availableFromDate = cand.isBefore(LocalDate.now()) ? LocalDate.now().plusDays(turnoverBufferDays) : cand;
            }
            if (availableFromDate.isAfter(startDate)) {
                throw new BusinessRuleException("UNIT_UNAVAILABLE",
                        "Unit " + unit.getCode() + " is in cleaning buffer until " + availableFromDate);
            }
        }

        // 3. Check conflicting overlapping reservations
        List<Reservation> conflicts = reservationRepository.findConflictingReservations(
                unit.getId(), ACTIVE_RESERVATION_STATUSES, startDate, endDate
        );
        if (!conflicts.isEmpty()) {
            throw new BusinessRuleException("UNIT_UNAVAILABLE",
                    "Unit " + unit.getCode() + " is no longer available for the selected dates");
        }

        // 4. Calculate locked price snapshot
        PricingBreakdownDto pricing = pricingEngine.calculatePricing(unit, request.durationMonths(), startDate);

        // 5. Create and persist Reservation
        String resCode = generateReservationCode();
        Reservation reservation = new Reservation(
                resCode,
                customer,
                unit,
                startDate,
                endDate,
                pricing.depositAmount(),
                null,
                ReservationStatus.PENDING_PAYMENT
        );
        reservation = reservationRepository.save(reservation);

        // 6. Create initial draft Contract holding immutable ContentSnapshot
        String contractCode = "CT-" + resCode.substring(3);
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("code", contractCode);
        snapshot.put("reservationCode", resCode);
        snapshot.put("unitCode", unit.getCode());
        snapshot.put("monthlyRate", pricing.monthlyRate().longValue());
        snapshot.put("baseRent", pricing.baseRent().longValue());
        snapshot.put("totalRent", pricing.totalRent().longValue());
        snapshot.put("depositAmount", pricing.depositAmount().longValue());
        snapshot.put("depositRate", pricing.depositRate().longValue());
        snapshot.put("durationMonths", request.durationMonths());
        snapshot.put("policyVersion", pricing.policyVersion());
        snapshot.put("currency", pricing.currency());

        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            snapshotJson = "{}";
        }

        Contract contract = new Contract(
                contractCode,
                reservation,
                activePolicy,
                snapshotJson,
                null,
                ContractStatus.DRAFT,
                null,
                1
        );
        contractRepository.save(contract);

        // 7. Append audit log
        logService.append(customer.getId(), EntityType.RESERVATION, reservation.getId(),
                Action.STATUS_CHANGE, null, ReservationStatus.PENDING_PAYMENT.name(), "Reservation created");

        // 8. Map and return DTO
        return mapToDto(reservation, unit, customer, pricing.monthlyRate().longValue(),
                pricing.baseRent().longValue(), pricing.totalRent().longValue(),
                pricing.depositAmount().longValue(), pricing.policyVersion(), request.durationMonths());
    }

    /**
     * Gets reservation details by ID with price snapshot.
     */
    @Transactional(readOnly = true)
    public ReservationDto getReservation(Long id, Long currentUserId, boolean isStaffOrAdmin) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation " + id + " not found"));

        if (!isStaffOrAdmin && !reservation.getCustomer().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Access denied to reservation " + id);
        }

        Unit unit = reservation.getUnit();
        User customer = reservation.getCustomer();

        // Extract snapshot from latest contract
        Contract contract = contractRepository.findLatestByReservationId(reservation.getId()).orElse(null);
        Long monthlyRate = null;
        Long baseRent = null;
        Long totalRent = null;
        String policyVersion = "v3";
        Integer durationMonths = 1;

        if (contract != null && contract.getContentSnapshot() != null) {
            try {
                Map<String, Object> snapshot = objectMapper.readValue(contract.getContentSnapshot(),
                        new TypeReference<>() {});
                if (snapshot.get("monthlyRate") != null) monthlyRate = ((Number) snapshot.get("monthlyRate")).longValue();
                if (snapshot.get("baseRent") != null) baseRent = ((Number) snapshot.get("baseRent")).longValue();
                if (snapshot.get("totalRent") != null) totalRent = ((Number) snapshot.get("totalRent")).longValue();
                if (snapshot.get("policyVersion") != null) policyVersion = (String) snapshot.get("policyVersion");
                if (snapshot.get("durationMonths") != null) durationMonths = ((Number) snapshot.get("durationMonths")).intValue();
            } catch (JsonProcessingException ignored) {
            }
        }

        if (monthlyRate == null || baseRent == null || totalRent == null) {
            // Fallback calculation if snapshot missing
            PricingBreakdownDto pricing = pricingEngine.calculatePricing(unit, durationMonths, reservation.getStartDate());
            monthlyRate = pricing.monthlyRate().longValue();
            baseRent = pricing.baseRent().longValue();
            totalRent = pricing.totalRent().longValue();
            policyVersion = pricing.policyVersion();
        }

        return mapToDto(reservation, unit, customer, monthlyRate, baseRent, totalRent,
                reservation.getDepositAmount().longValue(), policyVersion, durationMonths);
    }

    private String generateReservationCode() {
        // Form: BK-XXXX-XXXX (fits in VARCHAR(20))
        String raw = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "BK-" + raw;
    }

    private ReservationDto mapToDto(Reservation reservation, Unit unit, User customer,
                                   Long monthlyRate, Long baseRent, Long totalRent,
                                   Long depositAmount, String policyVersion, int durationMonths) {
        String typeName = unit.getUnitType() != null ? unit.getUnitType().getName() : null;
        String facilityName = unit.getZone() != null && unit.getZone().getFacility() != null
                ? unit.getZone().getFacility().getName() : "Tan Binh Depot";
        String facilityAddress = unit.getZone() != null && unit.getZone().getFacility() != null
                ? unit.getZone().getFacility().getAddress() : "45 Nguyen Van Troi, Tan Binh, Ho Chi Minh City";
        String zoneCode = unit.getZone() != null ? unit.getZone().getCode() : "A";
        Double sizeM2 = unit.getSizeM2() != null ? unit.getSizeM2().doubleValue() : null;

        return new ReservationDto(
                reservation.getId(),
                reservation.getCode(),
                customer.getId(),
                customer.getFullName(),
                unit.getId(),
                unit.getCode(),
                typeName,
                facilityName,
                facilityAddress,
                zoneCode,
                unit.getFloor(),
                sizeM2,
                unit.getAccessType(),
                reservation.getStartDate(),
                reservation.getEndDate(),
                durationMonths,
                depositAmount,
                monthlyRate,
                baseRent,
                totalRent,
                policyVersion,
                reservation.getAccessCode(),
                reservation.getStatus()
        );
    }
}
