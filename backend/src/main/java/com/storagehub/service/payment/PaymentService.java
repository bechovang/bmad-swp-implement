package com.storagehub.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storagehub.dto.CreatePaymentRequest;
import com.storagehub.dto.PaymentDto;
import com.storagehub.dto.PaymentResponseDto;
import com.storagehub.entity.Action;
import com.storagehub.entity.EntityType;
import com.storagehub.entity.Payment;
import com.storagehub.entity.PaymentMethod;
import com.storagehub.entity.PaymentPurpose;
import com.storagehub.entity.PaymentStatus;
import com.storagehub.entity.Reservation;
import com.storagehub.entity.ReservationStatus;
import com.storagehub.entity.Unit;
import com.storagehub.entity.UnitStatus;
import com.storagehub.entity.User;
import com.storagehub.exception.BusinessRuleException;
import com.storagehub.exception.ResourceNotFoundException;
import com.storagehub.repository.PaymentRepository;
import com.storagehub.repository.ReservationRepository;
import com.storagehub.repository.UnitRepository;
import com.storagehub.repository.UserRepository;
import com.storagehub.service.LogService;
import com.storagehub.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Transactional
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final UnitRepository unitRepository;
    private final PaymentGateway paymentGateway;
    private final PayOS payOS;
    private final LogService logService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public PaymentService(PaymentRepository paymentRepository,
                          ReservationRepository reservationRepository,
                          UserRepository userRepository,
                          UnitRepository unitRepository,
                          PaymentGateway paymentGateway,
                          PayOS payOS,
                          LogService logService,
                          NotificationService notificationService,
                          ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.unitRepository = unitRepository;
        this.paymentGateway = paymentGateway;
        this.payOS = payOS;
        this.logService = logService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    /**
     * Initiates a payment transaction (PayOS QR or counter Cash).
     */
    public PaymentResponseDto createPayment(CreatePaymentRequest request, Long currentUserId) {
        Reservation reservation = reservationRepository.findById(request.reservationId())
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + request.reservationId()));

        if (reservation.getStatus() != ReservationStatus.PENDING_PAYMENT) {
            throw new BusinessRuleException("PAYMENT_NOT_ALLOWED",
                    "Reservation " + reservation.getCode() + " is currently " + reservation.getStatus() + " and not awaiting payment");
        }

        User payer = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        long amount = request.amount() != null && request.amount() > 0
                ? request.amount()
                : reservation.getDepositAmount().longValue();

        // Generate a unique numeric orderCode for PayOS
        long orderCode = generateOrderCode();
        String receiptCode = "RC-" + orderCode;

        PaymentStatus initialStatus = (request.method() == PaymentMethod.CASH)
                ? PaymentStatus.PENDING_CASH
                : PaymentStatus.PENDING;

        Payment payment = new Payment(
                receiptCode,
                payer,
                reservation,
                null,
                null,
                request.purpose(),
                request.method(),
                BigDecimal.valueOf(amount),
                initialStatus
        );
        payment = paymentRepository.save(payment);

        String checkoutUrl = null;
        String qrCode = null;
        Instant expiresAt = null;

        if (request.method() == PaymentMethod.PAYOS) {
            String description = "StorageHub " + reservation.getCode();
            if (description.length() > 25) {
                description = description.substring(0, 25);
            }
            String returnUrl = "https://bechovang.loca.lt/rentals/" + reservation.getId();
            String cancelUrl = "https://bechovang.loca.lt/booking/summary?unit=" + reservation.getUnit().getCode();

            PaymentLinkResult linkResult = paymentGateway.createPaymentLink(
                    orderCode,
                    (int) amount,
                    description,
                    returnUrl,
                    cancelUrl
            );

            checkoutUrl = linkResult.checkoutUrl();
            qrCode = linkResult.qrCode();
            expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
        }

        return new PaymentResponseDto(
                payment.getId(),
                orderCode,
                amount,
                payment.getStatus(),
                payment.getMethod(),
                payment.getPurpose(),
                checkoutUrl,
                qrCode,
                expiresAt
        );
    }

    /**
     * Public Webhook endpoint handler: verifies PayOS checksum and updates state idempotently.
     */
    public void processPayOsWebhook(String rawPayload) {
        Webhook webhookBody;
        try {
            webhookBody = objectMapper.readValue(rawPayload, Webhook.class);
        } catch (Exception e) {
            throw new BusinessRuleException("MALFORMED_WEBHOOK", "Invalid webhook JSON structure: " + e.getMessage());
        }

        // Verify checksum using PayOS SDK webhooks().verify(payload)
        WebhookData verifiedData;
        try {
            verifiedData = payOS.webhooks().verify(webhookBody);
        } catch (Exception e) {
            log.error("PayOS webhook verification failed: {}", e.getMessage());
            throw new BusinessRuleException("INVALID_WEBHOOK_SIGNATURE", "Webhook checksum verification failed: " + e.getMessage());
        }

        if (verifiedData == null || verifiedData.getOrderCode() == null || verifiedData.getOrderCode() <= 0) {
            log.warn("PayOS webhook received without valid orderCode");
            return;
        }

        long orderCode = verifiedData.getOrderCode();
        String receiptCode = "RC-" + orderCode;
        Payment payment = paymentRepository.findByReceiptCode(receiptCode).orElse(null);

        if (payment == null) {
            log.warn("Payment with receipt code {} not found for webhook", receiptCode);
            return;
        }

        // Idempotency check: if already SUCCEEDED, return immediately
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            log.info("Payment {} is already SUCCEEDED. Skipping duplicate webhook.", payment.getId());
            return;
        }

        boolean isSuccess = "00".equals(webhookBody.getCode()) || "00".equals(verifiedData.getCode());
        if (isSuccess) {
            completeSuccessfulPayment(payment, PaymentMethod.PAYOS);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.info("Payment {} marked FAILED based on webhook code {}", payment.getId(), webhookBody.getCode());
        }
    }

    /**
     * Staff confirmation of cash received at counter.
     */
    public PaymentDto confirmCashPayment(Long paymentId, Long staffUserId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment " + paymentId + " not found"));

        if (payment.getStatus() != PaymentStatus.PENDING_CASH && payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessRuleException("INVALID_PAYMENT_STATUS",
                    "Payment " + paymentId + " is not awaiting cash confirmation (current: " + payment.getStatus() + ")");
        }

        completeSuccessfulPayment(payment, PaymentMethod.CASH);

        logService.append(staffUserId, EntityType.PAYMENT, payment.getId(),
                Action.STATUS_CHANGE, PaymentStatus.PENDING_CASH.name(), PaymentStatus.SUCCEEDED.name(),
                "Cash received confirmed by staff");

        return mapToDto(payment);
    }

    /**
     * Atomically executes business state transitions upon payment success.
     */
    private void completeSuccessfulPayment(Payment payment, PaymentMethod method) {
        PaymentStatus previousStatus = payment.getStatus();
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setMethod(method);
        paymentRepository.save(payment);

        // Advance Reservation and Unit states if purpose is DEPOSIT
        Reservation reservation = payment.getReservation();
        if (reservation != null && payment.getPurpose() == PaymentPurpose.DEPOSIT) {
            reservation.setStatus(ReservationStatus.RESERVED);
            reservationRepository.save(reservation);

            Unit unit = reservation.getUnit();
            if (unit != null) {
                unit.setStatus(UnitStatus.RESERVED);
                unitRepository.save(unit);
            }

            // Append audit logs
            logService.append(payment.getPayer().getId(), EntityType.PAYMENT, payment.getId(),
                    Action.STATUS_CHANGE, previousStatus.name(), PaymentStatus.SUCCEEDED.name(),
                    "Payment succeeded via " + method.name());

            logService.append(payment.getPayer().getId(), EntityType.RESERVATION, reservation.getId(),
                    Action.STATUS_CHANGE, ReservationStatus.PENDING_PAYMENT.name(), ReservationStatus.RESERVED.name(),
                    "Deposit confirmed - reservation locked");

            // Dispatch notification
            String notifTitle = "Deposit received - " + payment.getAmount() + " VND for unit " +
                    (unit != null ? unit.getCode() : "");
            notificationService.send(
                    payment.getPayer().getId(),
                    "PAYMENT_SUCCEEDED",
                    notifTitle,
                    "/rentals/" + reservation.getId()
            );
        }
    }

    @Transactional(readOnly = true)
    public PaymentDto getPayment(Long id, Long currentUserId, boolean isStaffOrAdmin) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment " + id + " not found"));

        if (!isStaffOrAdmin && !payment.getPayer().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Access denied to payment " + id);
        }

        return mapToDto(payment);
    }

    private long generateOrderCode() {
        long timestamp = System.currentTimeMillis() % 10000000000L;
        int randomSuffix = (int) (Math.random() * 900 + 100);
        return (timestamp / 1000) * 1000 + randomSuffix;
    }

    private PaymentDto mapToDto(Payment payment) {
        Long orderCode = null;
        if (payment.getReceiptCode() != null && payment.getReceiptCode().startsWith("RC-")) {
            try {
                orderCode = Long.parseLong(payment.getReceiptCode().substring(3));
            } catch (NumberFormatException ignored) {
            }
        }

        return new PaymentDto(
                payment.getId(),
                payment.getReceiptCode(),
                payment.getPayer().getId(),
                payment.getReservation() != null ? payment.getReservation().getId() : null,
                orderCode,
                payment.getPurpose(),
                payment.getMethod(),
                payment.getAmount().longValue(),
                payment.getStatus()
        );
    }
}
