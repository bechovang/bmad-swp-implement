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

    public SettlementService(ReservationRepository reservationRepository,
                             SettlementRepository settlementRepository,
                             PaymentRepository paymentRepository,
                             ContractRepository contractRepository,
                             UnitRepository unitRepository,
                             UserRepository userRepository,
                             TaskRepository taskRepository,
                             TaskService taskService,
                             LogService logService,
                             NotificationService notificationService) {
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
    }

    /**
     * Calculates the real-time financial settlement preview for a reservation.
     */
    @Transactional(readOnly = true)
    public SettlementPreviewDto calculatePreview(Long reservationId, BigDecimal customDamageFee,
                                                 String customDamageReason, LocalDate checkoutDate) {
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

        BigDecimal totalCharges = damageFee.add(lateFee);

        BigDecimal refundAmount;
        BigDecimal extraFeeAmount;

        if (depositHeld.compareTo(totalCharges) >= 0) {
            refundAmount = depositHeld.subtract(totalCharges);
            extraFeeAmount = BigDecimal.ZERO;
        } else {
            refundAmount = BigDecimal.ZERO;
            extraFeeAmount = totalCharges.subtract(depositHeld);
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
        boolean hasValidReason = !damageReasonRequired || (customDamageReason != null && !customDamageReason.trim().isEmpty());

        boolean canFinalize = hasValidReason && (!extraFeeRequired || extraFeePaid);

        String summaryMessage = buildSummaryMessage(depositHeld, damageFee, lateFee, refundAmount, extraFeeAmount, extraFeePaid);

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
                totalCharges,
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
     * Finalizes settlement and closes the rental in an atomic transaction (Story 6.3).
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

        BigDecimal totalCharges = damageFee.add(lateFee);

        BigDecimal refundAmount;
        BigDecimal extraFeeAmount;

        if (depositHeld.compareTo(totalCharges) >= 0) {
            refundAmount = depositHeld.subtract(totalCharges);
            extraFeeAmount = BigDecimal.ZERO;
        } else {
            refundAmount = BigDecimal.ZERO;
            extraFeeAmount = totalCharges.subtract(depositHeld);
        }

        User staff = resolveStaffUser(staffUserId);

        // 2. Extra Fee Payment Verification or Cash Collection
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

        // 3. Create Settlement Record
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
                refundAmount,
                extraFeeAmount,
                SettlementStatus.FINALIZED,
                settlementReceiptCode,
                request.notes()
        );
        settlement = settlementRepository.save(settlement);

        // 4. Flip Reservation -> CLOSED
        ReservationStatus previousResStatus = reservation.getStatus();
        reservation.setStatus(ReservationStatus.CLOSED);
        reservationRepository.save(reservation);

        // 5. Flip Contract -> CLOSED (if ACTIVE)
        if (contract != null && contract.getStatus() == ContractStatus.ACTIVE) {
            contract.setStatus(ContractStatus.CLOSED);
            contractRepository.save(contract);
        }

        // 6. Flip Unit -> PREPARING and spawn CLEANING task
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

        // 7. Complete CHECKOUT task on Kanban
        Optional<Task> checkoutTask = taskRepository.findByRefCodeAndType(reservation.getCode(), TaskType.CHECKOUT);
        if (checkoutTask.isPresent()) {
            Task ct = checkoutTask.get();
            if (ct.getStatus() != TaskStatus.DONE) {
                ct.setStatus(TaskStatus.DONE);
                taskRepository.save(ct);
            }
        }

        // 8. Audit Logs
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

        logService.append(
                staff.getId(),
                EntityType.RESERVATION,
                reservation.getId(),
                Action.STATUS_CHANGE,
                previousResStatus.name(),
                ReservationStatus.CLOSED.name(),
                "Rental finalized with settlement receipt " + settlementReceiptCode
        );

        // 9. Customer Notification
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

        String summaryMessage = buildSummaryMessage(
                settlement.getDepositHeld(),
                settlement.getDamageFee(),
                settlement.getLateFee(),
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
                settlement.getDamageFee().add(settlement.getLateFee()),
                settlement.getRefundAmount(),
                settlement.getExtraFee(),
                settlement.getStatus().name(),
                settlement.getNotes(),
                settlement.getCreatedAt(),
                summaryMessage
        );
    }

    private String buildSummaryMessage(BigDecimal depositHeld, BigDecimal damageFee, BigDecimal lateFee,
                                       BigDecimal refundAmount, BigDecimal extraFeeAmount, boolean extraFeePaid) {
        if (extraFeeAmount.compareTo(BigDecimal.ZERO) > 0) {
            return "Extra fee " + formatVnd(extraFeeAmount) + (extraFeePaid ? " (Paid)" : " (Unpaid)");
        }
        if (damageFee.compareTo(BigDecimal.ZERO) > 0) {
            return "Refund " + formatVnd(refundAmount) + " after damage fee " + formatVnd(damageFee);
        }
        if (lateFee.compareTo(BigDecimal.ZERO) > 0) {
            return "Refund " + formatVnd(refundAmount) + " after late fee " + formatVnd(lateFee);
        }
        return "Full refund of " + formatVnd(refundAmount);
    }

    private String formatVnd(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        NumberFormat nf = NumberFormat.getInstance(Locale.GERMANY); // Uses dot as thousands separator
        return nf.format(amount.longValue()) + " ₫";
    }
}
