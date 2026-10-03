package com.storagehub.service;

import com.storagehub.dto.FinalizeSettlementRequest;
import com.storagehub.dto.SettlementPreviewDto;
import com.storagehub.dto.SettlementReceiptDto;
import com.storagehub.entity.Action;
import com.storagehub.entity.Contract;
import com.storagehub.entity.ContractStatus;
import com.storagehub.entity.EntityType;
import com.storagehub.entity.Payment;
import com.storagehub.entity.PaymentMethod;
import com.storagehub.entity.PaymentPurpose;
import com.storagehub.entity.PaymentStatus;
import com.storagehub.entity.PolicyRule;
import com.storagehub.entity.PolicyRuleType;
import com.storagehub.entity.PolicyStatus;
import com.storagehub.entity.RentalPolicy;
import com.storagehub.entity.Reservation;
import com.storagehub.entity.ReservationStatus;
import com.storagehub.entity.Settlement;
import com.storagehub.entity.SettlementStatus;
import com.storagehub.entity.Task;
import com.storagehub.entity.TaskStatus;
import com.storagehub.entity.TaskType;
import com.storagehub.entity.Unit;
import com.storagehub.entity.UnitStatus;
import com.storagehub.entity.User;
import com.storagehub.exception.BusinessRuleException;
import com.storagehub.exception.ResourceNotFoundException;
import com.storagehub.repository.ContractRepository;
import com.storagehub.repository.PaymentRepository;
import com.storagehub.repository.PolicyRuleRepository;
import com.storagehub.repository.RentalPolicyRepository;
import com.storagehub.repository.ReservationRepository;
import com.storagehub.repository.SettlementRepository;
import com.storagehub.repository.TaskRepository;
import com.storagehub.repository.UnitRepository;
import com.storagehub.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Transactional
public class SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);

    private final ReservationRepository reservationRepository;
    private final SettlementRepository settlementRepository;
    private final PaymentRepository paymentRepository;
    private final ContractRepository contractRepository;
    private final UnitRepository unitRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final TaskService taskService;
    private final LogService logService;
    private final NotificationService notificationService;
    private final RentalPolicyRepository rentalPolicyRepository;
    private final PolicyRuleRepository policyRuleRepository;

    public SettlementService(ReservationRepository reservationRepository,
                             SettlementRepository settlementRepository,
                             PaymentRepository paymentRepository,
                             ContractRepository contractRepository,
                             UnitRepository unitRepository,
                             UserRepository userRepository,
                             TaskRepository taskRepository,
                             TaskService taskService,
                             LogService logService,
                             NotificationService notificationService,
                             RentalPolicyRepository rentalPolicyRepository,
                             PolicyRuleRepository policyRuleRepository) {
        this.reservationRepository = reservationRepository;
        this.settlementRepository = settlementRepository;
        this.paymentRepository = paymentRepository;
        this.contractRepository = contractRepository;
        this.unitRepository = unitRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.taskService = taskService;
        this.logService = logService;
        this.notificationService = notificationService;
        this.rentalPolicyRepository = rentalPolicyRepository;
        this.policyRuleRepository = policyRuleRepository;
    }

    /**
     * Calculates the real-time financial settlement preview for a reservation.
     */
    @Transactional(readOnly = true)
    public SettlementPreviewDto calculatePreview(Long reservationId, BigDecimal customDamageFee,
                                                 String customDamageReason, LocalDate checkoutDate) {
        return calculatePreview(reservationId, customDamageFee, customDamageReason, BigDecimal.ZERO, null, checkoutDate);
    }

    /**
     * Calculates the real-time financial settlement preview with optional fee waiver.
     */
    @Transactional(readOnly = true)
    public SettlementPreviewDto calculatePreview(Long reservationId, BigDecimal customDamageFee,
                                                 String customDamageReason, BigDecimal customWaiverAmount,
                                                 String customWaiverReason, LocalDate checkoutDate) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));

        BigDecimal depositHeld = reservation.getDepositAmount() != null
                ? reservation.getDepositAmount()
                : BigDecimal.ZERO;

        // Auto-compute late fee if checkout date is after reservation end date
        LocalDate refDate = (checkoutDate != null) ? checkoutDate : LocalDate.now();
        int daysLate = 0;
        BigDecimal lateFee = BigDecimal.ZERO;

        if (reservation.getEndDate() != null && refDate.isAfter(reservation.getEndDate())) {
            daysLate = (int) ChronoUnit.DAYS.between(reservation.getEndDate(), refDate);
            if (daysLate > 0) {
                BigDecimal monthlyRate = reservation.getMonthlyRate() != null
                        ? reservation.getMonthlyRate()
                        : BigDecimal.valueOf(690000);
                BigDecimal dailyRate = monthlyRate.divide(BigDecimal.valueOf(30), 0, RoundingMode.HALF_UP);
                lateFee = dailyRate.multiply(BigDecimal.valueOf(daysLate));
            }
        }

        BigDecimal damageFee = (customDamageFee != null)
                ? customDamageFee.max(BigDecimal.ZERO)
                : BigDecimal.ZERO;

        BigDecimal waiverAmount = (customWaiverAmount != null)
                ? customWaiverAmount.max(BigDecimal.ZERO)
                : BigDecimal.ZERO;

        WaiverCapInfo capInfo = resolveWaiverCapInfo(reservation);
        BigDecimal waiverCap = capInfo.cap();
        String policyVersion = capInfo.policyVersion();

        BigDecimal grossCharges = damageFee.add(lateFee);
        BigDecimal netCharges = grossCharges.subtract(waiverAmount).max(BigDecimal.ZERO);

        BigDecimal refundAmount;
        BigDecimal extraFeeAmount;

        if (depositHeld.compareTo(netCharges) >= 0) {
            refundAmount = depositHeld.subtract(netCharges);
            extraFeeAmount = BigDecimal.ZERO;
        } else {
            refundAmount = BigDecimal.ZERO;
            extraFeeAmount = netCharges.subtract(depositHeld);
        }

        boolean extraFeeRequired = extraFeeAmount.compareTo(BigDecimal.ZERO) > 0;
        boolean extraFeePaid = false;

        if (extraFeeRequired) {
            List<Payment> payments = paymentRepository.findByReservationId(reservation.getId());
            extraFeePaid = payments.stream()
                    .anyMatch(p -> (p.getPurpose() == PaymentPurpose.EXTRA_FEE || p.getPurpose() == PaymentPurpose.DAMAGE_FEE)
                            && p.getStatus() == PaymentStatus.SUCCEEDED
                            && p.getAmount().compareTo(extraFeeAmount) >= 0);
        }

        boolean damageReasonRequired = damageFee.compareTo(BigDecimal.ZERO) > 0;
        boolean hasValidDamageReason = !damageReasonRequired || (customDamageReason != null && !customDamageReason.trim().isEmpty());

        boolean isWaiverExceeded = waiverAmount.compareTo(waiverCap) > 0;
        boolean waiverReasonRequired = waiverAmount.compareTo(BigDecimal.ZERO) > 0;
        boolean hasValidWaiverReason = !waiverReasonRequired || (customWaiverReason != null && !customWaiverReason.trim().isEmpty());

        boolean canFinalize = hasValidDamageReason && hasValidWaiverReason && !isWaiverExceeded && (!extraFeeRequired || extraFeePaid);

        String summaryMessage = buildSummaryMessage(depositHeld, damageFee, lateFee, waiverAmount, refundAmount, extraFeeAmount, extraFeePaid);

        String unitCode = reservation.getUnit() != null ? reservation.getUnit().getCode() : null;
        String customerName = reservation.getCustomer() != null ? reservation.getCustomer().getFullName() : null;

        return new SettlementPreviewDto(
                reservation.getId(),
                reservation.getCode(),
                unitCode,
                customerName,
                depositHeld,
                damageFee,
                customDamageReason,
                lateFee,
                daysLate,
                waiverAmount,
                customWaiverReason,
                waiverCap,
                policyVersion,
                isWaiverExceeded,
                waiverReasonRequired,
                netCharges,
                refundAmount,
                extraFeeAmount,
                extraFeeRequired,
                extraFeePaid,
                damageReasonRequired,
                canFinalize,
                summaryMessage
        );
    }

    /**
     * Finalizes settlement and closes the rental in an atomic transaction (Story 6.3 & 6.4).
     */
    public SettlementReceiptDto finalizeSettlement(Long reservationId, FinalizeSettlementRequest request, Long staffUserId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));

        if (reservation.getStatus() == ReservationStatus.CLOSED) {
            // Already finalized, return existing receipt
            return settlementRepository.findByReservationIdWithDetails(reservationId)
                    .map(this::mapToReceiptDto)
                    .orElseThrow(() -> new BusinessRuleException("SETTLEMENT_ALREADY_CLOSED", "Reservation is already closed"));
        }

        // Exclusion Guard: Waiver cannot be applied to deposit forfeiture for no-show (FR-36)
        if (reservation.getStatus() == ReservationStatus.EXPIRED) {
            throw new BusinessRuleException("WAIVER_NOT_ALLOWED_NO_SHOW",
                    "Waiver cannot be applied to deposit forfeiture for no-show.");
        }

        BigDecimal damageFee = (request.damageFee() != null) ? request.damageFee().max(BigDecimal.ZERO) : BigDecimal.ZERO;
        String damageReason = request.damageReason();

        // 1. Mandatory Reason Check when damageFee > 0
        if (damageFee.compareTo(BigDecimal.ZERO) > 0 && (damageReason == null || damageReason.trim().isEmpty())) {
            throw new BusinessRuleException("DAMAGE_REASON_REQUIRED",
                    "A specific damage reason is mandatory when assessing damage fees.");
        }

        BigDecimal depositHeld = reservation.getDepositAmount() != null
                ? reservation.getDepositAmount()
                : BigDecimal.ZERO;

        BigDecimal lateFee = (request.lateFee() != null) ? request.lateFee().max(BigDecimal.ZERO) : BigDecimal.ZERO;
        if (lateFee.compareTo(BigDecimal.ZERO) == 0 && reservation.getEndDate() != null && LocalDate.now().isAfter(reservation.getEndDate())) {
            int daysLate = (int) ChronoUnit.DAYS.between(reservation.getEndDate(), LocalDate.now());
            if (daysLate > 0) {
                BigDecimal monthlyRate = reservation.getMonthlyRate() != null
                        ? reservation.getMonthlyRate()
                        : BigDecimal.valueOf(690000);
                BigDecimal dailyRate = monthlyRate.divide(BigDecimal.valueOf(30), 0, RoundingMode.HALF_UP);
                lateFee = dailyRate.multiply(BigDecimal.valueOf(daysLate));
            }
        }

        // 2. Waiver validation (Story 6.4)
        BigDecimal waiverAmount = (request.waiverAmount() != null) ? request.waiverAmount().max(BigDecimal.ZERO) : BigDecimal.ZERO;
        String waiverReason = request.waiverReason();

        WaiverCapInfo capInfo = resolveWaiverCapInfo(reservation);
        if (waiverAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (waiverReason == null || waiverReason.trim().isEmpty()) {
                throw new BusinessRuleException("WAIVER_REASON_REQUIRED",
                        "A specific waiver reason is mandatory when applying a fee waiver.");
            }
            if (waiverAmount.compareTo(capInfo.cap()) > 0) {
                throw new BusinessRuleException("WAIVER_EXCEEDS_CAP",
                        "Waiver exceeds the " + formatVnd(capInfo.cap()) + " cap in " + capInfo.policyVersion());
            }
        }

        BigDecimal grossCharges = damageFee.add(lateFee);
        BigDecimal netCharges = grossCharges.subtract(waiverAmount).max(BigDecimal.ZERO);

        BigDecimal refundAmount;
        BigDecimal extraFeeAmount;

        if (depositHeld.compareTo(netCharges) >= 0) {
            refundAmount = depositHeld.subtract(netCharges);
            extraFeeAmount = BigDecimal.ZERO;
        } else {
            refundAmount = BigDecimal.ZERO;
            extraFeeAmount = netCharges.subtract(depositHeld);
        }

        User staff = resolveStaffUser(staffUserId);

        // 3. Extra Fee Payment Verification or Cash Collection
        if (extraFeeAmount.compareTo(BigDecimal.ZERO) > 0) {
            boolean isCashCollection = Boolean.TRUE.equals(request.cashReceived())
                    || "CASH".equalsIgnoreCase(request.paymentMethod());

            if (isCashCollection) {
                // Record cash payment at desk
                String receiptCode = "RC-CASH-" + System.currentTimeMillis();
                Payment cashPayment = new Payment(
                        receiptCode,
                        reservation.getCustomer() != null ? reservation.getCustomer() : staff,
                        reservation,
                        null,
                        null,
                        PaymentPurpose.EXTRA_FEE,
                        PaymentMethod.CASH,
                        extraFeeAmount,
                        PaymentStatus.SUCCEEDED
                );
                paymentRepository.save(cashPayment);

                logService.append(
                        staff.getId(),
                        EntityType.PAYMENT,
                        cashPayment.getId(),
                        Action.STATUS_CHANGE,
                        PaymentStatus.PENDING_CASH.name(),
                        PaymentStatus.SUCCEEDED.name(),
                        "Cash extra fee received at desk: " + extraFeeAmount + " VND"
                );
            } else {
                List<Payment> payments = paymentRepository.findByReservationId(reservation.getId());
                boolean hasPaid = payments.stream()
                        .anyMatch(p -> (p.getPurpose() == PaymentPurpose.EXTRA_FEE || p.getPurpose() == PaymentPurpose.DAMAGE_FEE)
                                && p.getStatus() == PaymentStatus.SUCCEEDED
                                && p.getAmount().compareTo(extraFeeAmount) >= 0);

                if (!hasPaid) {
                    throw new BusinessRuleException("EXTRA_FEE_UNPAID",
                            "Outstanding extra fee of " + formatVnd(extraFeeAmount) + " must be paid before closing rental.");
                }
            }
        }

        // 4. Create Settlement Record
        Contract contract = contractRepository.findLatestByReservationId(reservation.getId()).orElse(null);
        String settlementReceiptCode = generateSettlementReceiptCode();

        Settlement settlement = new Settlement(
                reservation,
                contract,
                staff,
                depositHeld,
                damageFee,
                damageReason,
                lateFee,
                waiverAmount,
                waiverReason,
                refundAmount,
                extraFeeAmount,
                SettlementStatus.FINALIZED,
                settlementReceiptCode,
                request.notes()
        );
        settlement = settlementRepository.save(settlement);

        // 5. Flip Reservation -> CLOSED
        ReservationStatus previousResStatus = reservation.getStatus();
        reservation.setStatus(ReservationStatus.CLOSED);
        reservationRepository.save(reservation);

        // 6. Flip Contract -> CLOSED (if ACTIVE)
        if (contract != null && contract.getStatus() == ContractStatus.ACTIVE) {
            contract.setStatus(ContractStatus.CLOSED);
            contractRepository.save(contract);
        }

        // 7. Flip Unit -> PREPARING and spawn CLEANING task
        Unit unit = reservation.getUnit();
        if (unit != null) {
            unit.setStatus(UnitStatus.PREPARING);
            unitRepository.save(unit);

            taskService.createCleaningTask(unit);

            logService.append(
                    staff.getId(),
                    EntityType.UNIT,
                    unit.getId(),
                    Action.STATUS_CHANGE,
                    UnitStatus.RENTED.name(),
                    UnitStatus.PREPARING.name(),
                    "Unit vacated and set to PREPARING for cleaning"
            );
        }

        // 8. Complete CHECKOUT task on Kanban
        Optional<Task> checkoutTask = taskRepository.findByRefCodeAndType(reservation.getCode(), TaskType.CHECKOUT);
        if (checkoutTask.isPresent()) {
            Task ct = checkoutTask.get();
            if (ct.getStatus() != TaskStatus.DONE) {
                ct.setStatus(TaskStatus.DONE);
                taskRepository.save(ct);
            }
        }

        // 9. Audit Logs (Damage Charge + Waiver)
        if (damageFee.compareTo(BigDecimal.ZERO) > 0) {
            logService.append(
                    staff.getId(),
                    EntityType.SETTLEMENT,
                    settlement.getId(),
                    Action.DAMAGE_CHARGE,
                    null,
                    damageFee.toString(),
                    damageReason
            );
        }

        if (waiverAmount.compareTo(BigDecimal.ZERO) > 0) {
            logService.append(
                    staff.getId(),
                    EntityType.SETTLEMENT,
                    settlement.getId(),
                    Action.WAIVER,
                    null,
                    waiverAmount.toString(),
                    waiverReason
            );
        }

        logService.append(
                staff.getId(),
                EntityType.RESERVATION,
                reservation.getId(),
                Action.STATUS_CHANGE,
                previousResStatus.name(),
                ReservationStatus.CLOSED.name(),
                "Rental finalized with settlement receipt " + settlementReceiptCode
        );

        // 10. Customer Notification
        if (reservation.getCustomer() != null) {
            String notifMsg = (refundAmount.compareTo(BigDecimal.ZERO) > 0)
                    ? "Settlement receipt " + settlementReceiptCode + ": Refund of " + formatVnd(refundAmount) + " processed."
                    : "Settlement receipt " + settlementReceiptCode + ": Rental closed successfully.";

            notificationService.send(
                    reservation.getCustomer().getId(),
                    "SETTLEMENT_FINALIZED",
                    notifMsg,
                    "/rentals/" + reservation.getId()
            );
        }

        return mapToReceiptDto(settlement);
    }

    /**
     * Get permanent settlement receipt by reservation ID.
     */
    @Transactional(readOnly = true)
    public SettlementReceiptDto getSettlementByReservationId(Long reservationId) {
        Settlement settlement = settlementRepository.findByReservationIdWithDetails(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found for reservation: " + reservationId));
        return mapToReceiptDto(settlement);
    }

    private User resolveStaffUser(Long staffUserId) {
        if (staffUserId != null) {
            return userRepository.findById(staffUserId)
                    .orElseGet(() -> userRepository.findAll().stream().findFirst()
                            .orElseThrow(() -> new ResourceNotFoundException("Staff user not found")));
        }
        return userRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No staff user found"));
    }

    private String generateSettlementReceiptCode() {
        int year = LocalDate.now().getYear();
        int randomNum = (int) (Math.random() * 90000 + 10000);
        return "STL-" + year + "-" + randomNum;
    }

    private SettlementReceiptDto mapToReceiptDto(Settlement settlement) {
        Reservation res = settlement.getReservation();
        String unitCode = (res != null && res.getUnit() != null) ? res.getUnit().getCode() : null;
        String customerName = (res != null && res.getCustomer() != null) ? res.getCustomer().getFullName() : null;
        String staffName = (settlement.getStaff() != null) ? settlement.getStaff().getFullName() : null;

        BigDecimal grossCharges = settlement.getDamageFee().add(settlement.getLateFee());
        BigDecimal netCharges = grossCharges.subtract(settlement.getWaiverAmount()).max(BigDecimal.ZERO);

        String summaryMessage = buildSummaryMessage(
                settlement.getDepositHeld(),
                settlement.getDamageFee(),
                settlement.getLateFee(),
                settlement.getWaiverAmount(),
                settlement.getRefundAmount(),
                settlement.getExtraFee(),
                true
        );

        return new SettlementReceiptDto(
                settlement.getId(),
                settlement.getReceiptCode(),
                res != null ? res.getId() : null,
                res != null ? res.getCode() : null,
                unitCode,
                customerName,
                staffName,
                settlement.getDepositHeld(),
                settlement.getDamageFee(),
                settlement.getDamageReason(),
                settlement.getLateFee(),
                settlement.getWaiverAmount(),
                settlement.getWaiverReason(),
                netCharges,
                settlement.getRefundAmount(),
                settlement.getExtraFee(),
                settlement.getStatus().name(),
                settlement.getNotes(),
                settlement.getCreatedAt(),
                summaryMessage
        );
    }

    private String buildSummaryMessage(BigDecimal depositHeld, BigDecimal damageFee, BigDecimal lateFee,
                                       BigDecimal waiverAmount, BigDecimal refundAmount, BigDecimal extraFeeAmount,
                                       boolean extraFeePaid) {
        if (extraFeeAmount.compareTo(BigDecimal.ZERO) > 0) {
            return "Extra fee " + formatVnd(extraFeeAmount) + (extraFeePaid ? " (Paid)" : " (Unpaid)");
        }
        if (damageFee.compareTo(BigDecimal.ZERO) > 0 || lateFee.compareTo(BigDecimal.ZERO) > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("Refund ").append(formatVnd(refundAmount)).append(" after ");
            if (damageFee.compareTo(BigDecimal.ZERO) > 0 && lateFee.compareTo(BigDecimal.ZERO) > 0) {
                sb.append("damage fee ").append(formatVnd(damageFee)).append(" and late fee ").append(formatVnd(lateFee));
            } else if (damageFee.compareTo(BigDecimal.ZERO) > 0) {
                sb.append("damage fee ").append(formatVnd(damageFee));
            } else {
                sb.append("late fee ").append(formatVnd(lateFee));
            }
            if (waiverAmount != null && waiverAmount.compareTo(BigDecimal.ZERO) > 0) {
                sb.append(" (waived ").append(formatVnd(waiverAmount)).append(")");
            }
            return sb.toString();
        }
        return "Full refund of " + formatVnd(refundAmount);
    }

    public record WaiverCapInfo(BigDecimal cap, String policyVersion) {}

    public WaiverCapInfo resolveWaiverCapInfo(Reservation reservation) {
        LocalDate queryDate = (reservation != null && reservation.getStartDate() != null)
                ? reservation.getStartDate()
                : LocalDate.now();

        RentalPolicy activePolicy = null;
        if (rentalPolicyRepository != null) {
            activePolicy = rentalPolicyRepository
                    .findFirstByStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(PolicyStatus.ACTIVE, queryDate)
                    .orElse(null);
        }

        String policyVersion = (activePolicy != null && activePolicy.getVersion() != null)
                ? "Rental Policy " + activePolicy.getVersion()
                : "Rental Policy v3";

        BigDecimal waiverCap = BigDecimal.valueOf(50000); // default 50,000 VND
        if (activePolicy != null && policyRuleRepository != null) {
            Optional<PolicyRule> ruleOpt = policyRuleRepository
                    .findFirstByPolicy_IdAndRuleType(activePolicy.getId(), PolicyRuleType.WAIVER_CAP);
            if (ruleOpt.isPresent()) {
                PolicyRule rule = ruleOpt.get();
                if (rule.getCap() != null) {
                    waiverCap = rule.getCap();
                } else if (rule.getValue() != null) {
                    waiverCap = rule.getValue();
                }
            }
        }

        return new WaiverCapInfo(waiverCap, policyVersion);
    }

    private String formatVnd(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        NumberFormat nf = NumberFormat.getInstance(Locale.GERMANY); // Uses dot as thousands separator
        return nf.format(amount.longValue()) + " ₫";
    }
}
