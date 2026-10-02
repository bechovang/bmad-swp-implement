package com.storagehub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storagehub.dto.ContractContentSnapshotDto;
import com.storagehub.dto.ContractDto;
import com.storagehub.dto.PricingBreakdownDto;
import com.storagehub.entity.Action;
import com.storagehub.entity.Contract;
import com.storagehub.entity.ContractStatus;
import com.storagehub.entity.EntityType;
import com.storagehub.entity.RentalPolicy;
import com.storagehub.entity.Reservation;
import com.storagehub.entity.Unit;
import com.storagehub.entity.User;
import com.storagehub.exception.BusinessRuleException;
import com.storagehub.exception.ResourceNotFoundException;
import com.storagehub.repository.ContractRepository;
import com.storagehub.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ContractService {

    private static final Logger log = LoggerFactory.getLogger(ContractService.class);

    private final ContractRepository contractRepository;
    private final ReservationRepository reservationRepository;
    private final PricingEngine pricingEngine;
    private final LogService logService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public ContractService(ContractRepository contractRepository,
                           ReservationRepository reservationRepository,
                           PricingEngine pricingEngine,
                           LogService logService,
                           NotificationService notificationService,
                           ObjectMapper objectMapper) {
        this.contractRepository = contractRepository;
        this.reservationRepository = reservationRepository;
        this.pricingEngine = pricingEngine;
        this.logService = logService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    /**
     * Automatically creates an immutable DRAFT Contract within the transaction upon deposit payment success (Story 3.1).
     */
    public ContractDto createDraftContract(Reservation reservation) {
        // Idempotency: return existing latest contract if already created for this reservation
        Optional<Contract> existingOpt = contractRepository.findLatestByReservationId(reservation.getId());
        if (existingOpt.isPresent()) {
            return mapToDto(existingOpt.get());
        }

        Unit unit = reservation.getUnit();
        User customer = reservation.getCustomer();
        LocalDate startDate = reservation.getStartDate();
        LocalDate endDate = reservation.getEndDate();

        int durationMonths = calculateDurationMonths(startDate, endDate);
        RentalPolicy activePolicy = pricingEngine.resolveActivePolicy(startDate);
        PricingBreakdownDto pricing = pricingEngine.calculatePricing(unit, durationMonths, startDate);

        String contractCode = generateContractCode(reservation.getCode());

        String snapshotJson = buildSnapshotJson(
                contractCode,
                reservation.getCode(),
                unit,
                customer,
                startDate,
                endDate,
                durationMonths,
                pricing
        );

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
        contract = contractRepository.save(contract);

        // Audit log (EntityType.CONTRACT, Action.STATUS_CHANGE)
        logService.append(
                customer.getId(),
                EntityType.CONTRACT,
                contract.getId(),
                Action.STATUS_CHANGE,
                null,
                ContractStatus.DRAFT.name(),
                "Contract draft auto-generated from deposit payment"
        );

        // Customer notification
        String notifTitle = "Contract " + contractCode + " drafted from your booking and Rental Policy " +
                pricing.policyVersion() + ". You'll sign it at check-in.";
        notificationService.send(
                customer.getId(),
                "CONTRACT_DRAFTED",
                notifTitle,
                "/rentals/" + reservation.getId()
        );

        return mapToDto(contract);
    }

    /**
     * Reads a contract by ID with permission checks.
     */
    @Transactional(readOnly = true)
    public ContractDto getContract(Long id, Long currentUserId, boolean isStaffOrAdmin) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + id));

        validateAccess(contract.getReservation(), currentUserId, isStaffOrAdmin);

        return mapToDto(contract);
    }

    /**
     * Reads the latest contract for a reservation by reservation ID.
     */
    @Transactional(readOnly = true)
    public ContractDto getLatestContractByReservationId(Long reservationId, Long currentUserId, boolean isStaffOrAdmin) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));

        validateAccess(reservation, currentUserId, isStaffOrAdmin);

        Contract contract = contractRepository.findLatestByReservationId(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("No contract found for reservation: " + reservationId));

        return mapToDto(contract);
    }

    /**
     * Staff re-drafting: marks current contract as SUPERSEDED and creates a new DRAFT contract in the chain.
     */
    public ContractDto reDraftContract(Long contractId, Long staffUserId) {
        Contract previousContract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));

        if (previousContract.getStatus() != ContractStatus.DRAFT) {
            throw new BusinessRuleException(
                    "INVALID_CONTRACT_STATUS",
                    "Only DRAFT contracts can be re-drafted. Current status: " + previousContract.getStatus()
            );
        }

        if (previousContract.getIsLatest() != 1) {
            throw new BusinessRuleException(
                    "CONTRACT_NOT_LATEST",
                    "Only the latest contract version can be re-drafted"
            );
        }

        // 1. Mark previous contract as SUPERSEDED and clear isLatest
        previousContract.setStatus(ContractStatus.SUPERSEDED);
        previousContract.setIsLatest(0);
        contractRepository.saveAndFlush(previousContract);

        Reservation reservation = previousContract.getReservation();
        Unit unit = reservation.getUnit();
        User customer = reservation.getCustomer();
        LocalDate startDate = reservation.getStartDate();
        LocalDate endDate = reservation.getEndDate();

        int durationMonths = calculateDurationMonths(startDate, endDate);
        RentalPolicy activePolicy = pricingEngine.resolveActivePolicy(startDate);
        PricingBreakdownDto pricing = pricingEngine.calculatePricing(unit, durationMonths, startDate);

        long revisionCount = contractRepository.countByReservationId(reservation.getId());
        String newCode = generateRevisionCode(previousContract.getCode(), revisionCount);

        String snapshotJson = buildSnapshotJson(
                newCode,
                reservation.getCode(),
                unit,
                customer,
                startDate,
                endDate,
                durationMonths,
                pricing
        );

        // 2. Create new DRAFT contract linked to supersedesContract
        Contract newContract = new Contract(
                newCode,
                reservation,
                activePolicy,
                snapshotJson,
                null,
                ContractStatus.DRAFT,
                previousContract,
                1
        );
        newContract = contractRepository.save(newContract);

        // 3. Append audit logs
        logService.append(
                staffUserId,
                EntityType.CONTRACT,
                previousContract.getId(),
                Action.STATUS_CHANGE,
                ContractStatus.DRAFT.name(),
                ContractStatus.SUPERSEDED.name(),
                "Contract superseded by re-draft " + newContract.getCode()
        );

        logService.append(
                staffUserId,
                EntityType.CONTRACT,
                newContract.getId(),
                Action.STATUS_CHANGE,
                null,
                ContractStatus.DRAFT.name(),
                "New contract draft created via staff re-draft, superseding " + previousContract.getCode()
        );

        // 4. Notify customer of re-draft
        String notifTitle = "Contract " + newContract.getCode() + " drafted from your booking and Rental Policy " +
                pricing.policyVersion() + ". You'll sign it at check-in.";
        notificationService.send(
                customer.getId(),
                "CONTRACT_DRAFTED",
                notifTitle,
                "/rentals/" + reservation.getId()
        );

        return mapToDto(newContract);
    }

    /**
     * Mark contract as printed (DRAFT -> PRINTED)
     */
    public ContractDto markContractPrinted(Long contractId, Long staffUserId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));

        if (contract.getStatus() == ContractStatus.DRAFT) {
            contract.setStatus(ContractStatus.PRINTED);
            contractRepository.save(contract);

            logService.append(
                    staffUserId,
                    EntityType.CONTRACT,
                    contract.getId(),
                    Action.STATUS_CHANGE,
                    ContractStatus.DRAFT.name(),
                    ContractStatus.PRINTED.name(),
                    "Contract marked as PRINTED for front-desk ritual"
            );
        }

        return mapToDto(contract);
    }

    /**
     * Attach signed contract photo and transition status to SIGNED (Story 3.4)
     */
    public ContractDto signContract(Long contractId, String signedPhotoUrl, Long staffUserId) {
        if (signedPhotoUrl == null || signedPhotoUrl.trim().isEmpty()) {
            throw new BusinessRuleException("PHOTO_REQUIRED", "Signed contract photo attachment is required");
        }

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));

        if (contract.getStatus() == ContractStatus.SIGNED || contract.getStatus() == ContractStatus.ACTIVE) {
            // Idempotent: if already signed with the same photo, return current state
            return mapToDto(contract);
        }

        if (contract.getStatus() == ContractStatus.SUPERSEDED || contract.getStatus() == ContractStatus.CLOSED) {
            throw new BusinessRuleException("INVALID_CONTRACT_STATUS",
                    "Cannot sign a contract with status " + contract.getStatus());
        }

        String fromStatus = contract.getStatus().name();
        contract.setSignedPhotoUrl(signedPhotoUrl.trim());
        contract.setStatus(ContractStatus.SIGNED);
        contract = contractRepository.save(contract);

        // Audit log: CONTRACT_SIGNED
        logService.append(
                staffUserId,
                EntityType.CONTRACT,
                contract.getId(),
                Action.STATUS_CHANGE,
                fromStatus,
                ContractStatus.SIGNED.name(),
                "Contract signed with photo attachment " + signedPhotoUrl.trim()
        );

        // Notify customer
        Reservation reservation = contract.getReservation();
        if (reservation.getCustomer() != null) {
            notificationService.send(
                    reservation.getCustomer().getId(),
                    "CONTRACT_SIGNED",
                    "Contract " + contract.getCode() + " has been signed and recorded.",
                    "/rentals/" + reservation.getId()
            );
        }

        return mapToDto(contract);
    }

    /**
     * Get full contract revision chain for a reservation (Story 3.4).
     */
    @Transactional(readOnly = true)
    public java.util.List<ContractDto> getContractChain(Long reservationId, Long currentUserId, boolean isStaffOrAdmin) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));

        validateAccess(reservation, currentUserId, isStaffOrAdmin);

        java.util.List<Contract> contracts = contractRepository.findByReservation_Id(reservationId);
        return contracts.stream()
                .sorted(java.util.Comparator.comparing(Contract::getId))
                .map(this::mapToDto)
                .toList();
    }

    private void validateAccess(Reservation reservation, Long currentUserId, boolean isStaffOrAdmin) {
        if (!isStaffOrAdmin && (reservation.getCustomer() == null || !reservation.getCustomer().getId().equals(currentUserId))) {
            throw new AccessDeniedException("Access denied to contract for reservation " + reservation.getId());
        }
    }

    private int calculateDurationMonths(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return 1;
        }
        Period period = Period.between(startDate, endDate);
        int months = period.getYears() * 12 + period.getMonths();
        return Math.max(months, 1);
    }

    private String generateContractCode(String reservationCode) {
        String base = reservationCode != null && reservationCode.startsWith("BK-")
                ? reservationCode.substring(3)
                : (reservationCode != null ? reservationCode : UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        String code = "CT-" + base;
        if (code.length() > 20) {
            code = code.substring(0, 20);
        }
        if (contractRepository.findByCode(code).isPresent()) {
            String suffix = String.valueOf((int) (Math.random() * 900 + 100));
            code = "CT-" + base.substring(0, Math.min(base.length(), 13)) + "-" + suffix;
        }
        return code;
    }

    private String generateRevisionCode(String previousCode, long revisionCount) {
        String base = previousCode.contains("-R")
                ? previousCode.substring(0, previousCode.lastIndexOf("-R"))
                : previousCode;
        String revSuffix = "-R" + revisionCount;
        if ((base + revSuffix).length() > 20) {
            base = base.substring(0, 20 - revSuffix.length());
        }
        String candidate = base + revSuffix;
        if (contractRepository.findByCode(candidate).isPresent()) {
            candidate = base + "-R" + System.currentTimeMillis() % 10000;
            if (candidate.length() > 20) {
                candidate = candidate.substring(0, 20);
            }
        }
        return candidate;
    }

    private String buildSnapshotJson(String contractCode,
                                     String reservationCode,
                                     Unit unit,
                                     User customer,
                                     LocalDate startDate,
                                     LocalDate endDate,
                                     int durationMonths,
                                     PricingBreakdownDto pricing) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("code", contractCode);
        snapshot.put("reservationCode", reservationCode);
        snapshot.put("unitCode", unit != null ? unit.getCode() : "");
        snapshot.put("monthlyRate", pricing != null && pricing.monthlyRate() != null ? pricing.monthlyRate().longValue() : 0L);
        snapshot.put("baseRent", pricing != null && pricing.baseRent() != null ? pricing.baseRent().longValue() : 0L);
        snapshot.put("totalRent", pricing != null && pricing.totalRent() != null ? pricing.totalRent().longValue() : 0L);
        snapshot.put("depositAmount", pricing != null && pricing.depositAmount() != null ? pricing.depositAmount().longValue() : 0L);
        snapshot.put("depositRate", pricing != null && pricing.depositRate() != null ? pricing.depositRate().longValue() : 0L);
        snapshot.put("durationMonths", durationMonths);
        snapshot.put("policyVersion", pricing != null ? pricing.policyVersion() : "v3");
        snapshot.put("currency", pricing != null && pricing.currency() != null ? pricing.currency() : "VND");
        snapshot.put("startDate", startDate != null ? startDate.toString() : "");
        snapshot.put("endDate", endDate != null ? endDate.toString() : "");
        snapshot.put("customerName", customer != null ? customer.getFullName() : "");
        snapshot.put("customerEmail", customer != null ? customer.getEmail() : "");
        snapshot.put("customerPhone", customer != null ? customer.getPhone() : "");
        snapshot.put("facilityName", unit != null && unit.getZone() != null && unit.getZone().getFacility() != null
                ? unit.getZone().getFacility().getName() : "Tan Binh Depot");
        snapshot.put("facilityAddress", unit != null && unit.getZone() != null && unit.getZone().getFacility() != null
                ? unit.getZone().getFacility().getAddress() : "45 Nguyen Van Troi, Tan Binh, Ho Chi Minh City");
        snapshot.put("zoneCode", unit != null && unit.getZone() != null ? unit.getZone().getCode() : "A");
        snapshot.put("floor", unit != null ? unit.getFloor() : 1);
        snapshot.put("sizeM2", unit != null && unit.getSizeM2() != null ? unit.getSizeM2().doubleValue() : null);

        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize contract snapshot: {}", e.getMessage());
            return "{}";
        }
    }

    public ContractDto mapToDto(Contract contract) {
        ContractContentSnapshotDto snapshotDto = null;
        if (contract.getContentSnapshot() != null) {
            try {
                snapshotDto = objectMapper.readValue(contract.getContentSnapshot(), ContractContentSnapshotDto.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse contract content snapshot for contract {}: {}", contract.getId(), e.getMessage());
            }
        }

        Long supersedesId = contract.getSupersedesContract() != null ? contract.getSupersedesContract().getId() : null;
        String policyVersion = contract.getPolicy() != null ? contract.getPolicy().getVersion() : null;
        Integer policyId = contract.getPolicy() != null ? contract.getPolicy().getId() : null;

        return new ContractDto(
                contract.getId(),
                contract.getCode(),
                contract.getReservation().getId(),
                contract.getReservation().getCode(),
                policyId,
                policyVersion,
                contract.getContentSnapshot(),
                snapshotDto,
                contract.getSignedPhotoUrl(),
                contract.getStatus(),
                supersedesId,
                contract.getIsLatest()
        );
    }
}
