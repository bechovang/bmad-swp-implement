package com.storagehub.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storagehub.dto.CreatePaymentRequest;
import com.storagehub.dto.PaymentDto;
import com.storagehub.dto.PaymentResponseDto;
import com.storagehub.entity.Facility;
import com.storagehub.entity.Payment;
import com.storagehub.entity.PaymentMethod;
import com.storagehub.entity.PaymentPurpose;
import com.storagehub.entity.PaymentStatus;
import com.storagehub.entity.Reservation;
import com.storagehub.entity.ReservationStatus;
import com.storagehub.entity.Role;
import com.storagehub.entity.RoleName;
import com.storagehub.entity.Unit;
import com.storagehub.entity.UnitStatus;
import com.storagehub.entity.UnitType;
import com.storagehub.entity.User;
import com.storagehub.entity.UserStatus;
import com.storagehub.entity.Zone;
import com.storagehub.exception.BusinessRuleException;
import com.storagehub.repository.PaymentRepository;
import com.storagehub.repository.ReservationRepository;
import com.storagehub.repository.UnitRepository;
import com.storagehub.repository.UserRepository;
import com.storagehub.service.ContractService;
import com.storagehub.service.LogService;
import com.storagehub.service.NotificationService;
import com.storagehub.service.payment.PaymentGateway;
import com.storagehub.service.payment.PaymentLinkResult;
import com.storagehub.service.payment.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import vn.payos.PayOS;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;
import vn.payos.service.blocking.webhooks.WebhooksService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTests {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UnitRepository unitRepository;
    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private PayOS payOS;
    @Mock
    private LogService logService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ContractService contractService;
    @Mock
    private com.storagehub.service.TaskService taskService;

    private PaymentService paymentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private User customer;
    private User staff;
    private Unit unit;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                reservationRepository,
                userRepository,
                unitRepository,
                paymentGateway,
                payOS,
                logService,
                notificationService,
                contractService,
                taskService,
                objectMapper
        );

        Role customerRole = new Role(1, "Customer", "Customer role");
        Role staffRole = new Role(2, "Staff", "Staff role");

        customer = new User("Lan Nguyen", "lan@storagehub.dev", "0901234567", "hash", customerRole, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(customer, "id", 1L);

        staff = new User("Minh Tran", "minh@storagehub.dev", "0902345678", "hash", staffRole, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(staff, "id", 2L);

        Facility facility = new Facility("Tan Binh Depot", "45 Nguyen Van Troi", "02839991122", 1);
        Zone zone = new Zone(facility, "A", 1);
        UnitType unitType = new UnitType("S", "Small unit");
        ReflectionTestUtils.setField(unitType, "id", 2);

        unit = new Unit("S-3", unitType, zone, BigDecimal.valueOf(5.0), 1, "PIN", UnitStatus.AVAILABLE);
        ReflectionTestUtils.setField(unit, "id", 1L);

        reservation = new Reservation(
                "BK-2026-0001",
                customer,
                unit,
                LocalDate.of(2026, 10, 3),
                LocalDate.of(2026, 11, 3),
                BigDecimal.valueOf(103500),
                null,
                ReservationStatus.PENDING_PAYMENT
        );
        ReflectionTestUtils.setField(reservation, "id", 100L);
    }

    @Test
    @DisplayName("createPayment with PAYOS creates pending payment and PayOS link")
    void testCreatePaymentPayOs() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 10L);
            return p;
        });
        when(paymentGateway.createPaymentLink(anyLong(), anyInt(), anyString(), anyString(), anyString()))
                .thenReturn(new PaymentLinkResult("https://pay.payos.vn/checkout", "mock-qr", 123456789L, 103500, "PENDING"));

        CreatePaymentRequest request = new CreatePaymentRequest(100L, PaymentPurpose.DEPOSIT, PaymentMethod.PAYOS, 103500L);
        PaymentResponseDto response = paymentService.createPayment(request, 1L);

        assertThat(response).isNotNull();
        assertThat(response.paymentId()).isEqualTo(10L);
        assertThat(response.amount()).isEqualTo(103500L);
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.checkoutUrl()).isEqualTo("https://pay.payos.vn/checkout");
        assertThat(response.qrCode()).isEqualTo("mock-qr");
        assertThat(response.expiresAt()).isNotNull();
    }

    @Test
    @DisplayName("createPayment with CASH sets status to PENDING_CASH")
    void testCreatePaymentCash() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 11L);
            return p;
        });

        CreatePaymentRequest request = new CreatePaymentRequest(100L, PaymentPurpose.DEPOSIT, PaymentMethod.CASH, 103500L);
        PaymentResponseDto response = paymentService.createPayment(request, 1L);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING_CASH);
        assertThat(response.checkoutUrl()).isNull();
        verify(paymentGateway, never()).createPaymentLink(anyLong(), anyInt(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("processPayOsWebhook successfully marks payment SUCCEEDED and updates reservation")
    void testProcessPayOsWebhook() throws Exception {
        Payment payment = new Payment("RC-1727932800", customer, reservation, null, null,
                PaymentPurpose.DEPOSIT, PaymentMethod.PAYOS, BigDecimal.valueOf(103500), PaymentStatus.PENDING);
        ReflectionTestUtils.setField(payment, "id", 20L);

        when(paymentRepository.findByReceiptCode("RC-1727932800")).thenReturn(Optional.of(payment));

        WebhookData webhookData = new WebhookData(
                1727932800L, 103500L, "StorageHub BK-2026-0001", "1234567890",
                "REF123", "2026-10-03 00:00:00", "VND", "link123",
                "00", "success", "970400", "MBBank",
                "Lan Nguyen", "0901234567", "Virtual", "000"
        );
        WebhooksService webhooksService = org.mockito.Mockito.mock(WebhooksService.class);
        when(payOS.webhooks()).thenReturn(webhooksService);
        when(webhooksService.verify(any())).thenReturn(webhookData);

        String jsonPayload = """
                {
                  "code": "00",
                  "desc": "success",
                  "data": {
                    "orderCode": 1727932800,
                    "amount": 103500
                  },
                  "signature": "valid-sig"
                }
                """;

        paymentService.processPayOsWebhook(jsonPayload);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(unit.getStatus()).isEqualTo(UnitStatus.RESERVED);

        verify(paymentRepository).save(payment);
        verify(reservationRepository).save(reservation);
        verify(unitRepository).save(unit);
        verify(contractService).createDraftContract(reservation);
        verify(taskService).createCheckInTask(reservation);
        verify(notificationService).send(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("processPayOsWebhook is strictly idempotent when called twice")
    void testProcessPayOsWebhook_idempotent() throws Exception {
        Payment payment = new Payment("RC-1727932800", customer, reservation, null, null,
                PaymentPurpose.DEPOSIT, PaymentMethod.PAYOS, BigDecimal.valueOf(103500), PaymentStatus.SUCCEEDED);
        ReflectionTestUtils.setField(payment, "id", 20L);

        when(paymentRepository.findByReceiptCode("RC-1727932800")).thenReturn(Optional.of(payment));

        WebhookData webhookData = new WebhookData(
                1727932800L, 103500L, "StorageHub BK-2026-0001", "1234567890",
                "REF123", "2026-10-03 00:00:00", "VND", "link123",
                "00", "success", "970400", "MBBank",
                "Lan Nguyen", "0901234567", "Virtual", "000"
        );
        WebhooksService webhooksService = org.mockito.Mockito.mock(WebhooksService.class);
        when(payOS.webhooks()).thenReturn(webhooksService);
        when(webhooksService.verify(any())).thenReturn(webhookData);

        String jsonPayload = """
                {
                  "code": "00",
                  "desc": "success",
                  "data": {
                    "orderCode": 1727932800,
                    "amount": 103500
                  },
                  "signature": "valid-sig"
                }
                """;

        paymentService.processPayOsWebhook(jsonPayload);

        // Should not re-save or double process
        verify(reservationRepository, never()).save(any(Reservation.class));
        verify(contractService, never()).createDraftContract(any(Reservation.class));
        verify(taskService, never()).createCheckInTask(any(Reservation.class));
        verify(notificationService, never()).send(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("confirmCashPayment transitions PENDING_CASH to SUCCEEDED and updates reservation")
    void testConfirmCashPayment() {
        Payment payment = new Payment("RC-1727932801", customer, reservation, null, null,
                PaymentPurpose.DEPOSIT, PaymentMethod.CASH, BigDecimal.valueOf(103500), PaymentStatus.PENDING_CASH);
        ReflectionTestUtils.setField(payment, "id", 30L);

        when(paymentRepository.findById(30L)).thenReturn(Optional.of(payment));

        PaymentDto dto = paymentService.confirmCashPayment(30L, 2L);

        assertThat(dto).isNotNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(payment.getMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(unit.getStatus()).isEqualTo(UnitStatus.RESERVED);

        verify(paymentRepository).save(payment);
        verify(reservationRepository).save(reservation);
        verify(unitRepository).save(unit);
        verify(contractService).createDraftContract(reservation);
        verify(taskService).createCheckInTask(reservation);
    }
}
