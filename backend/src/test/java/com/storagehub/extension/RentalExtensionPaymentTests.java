package com.storagehub.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storagehub.dto.ContractDto;
import com.storagehub.dto.CreatePaymentRequest;
import com.storagehub.dto.PaymentDto;
import com.storagehub.dto.PaymentResponseDto;
import com.storagehub.entity.*;
import com.storagehub.exception.BusinessRuleException;
import com.storagehub.service.payment.PaymentGateway;
import com.storagehub.repository.PaymentRepository;
import com.storagehub.repository.ReservationRepository;
import com.storagehub.repository.UnitRepository;
import com.storagehub.repository.UserRepository;
import com.storagehub.service.ContractService;
import com.storagehub.service.LogService;
import com.storagehub.service.NotificationService;
import com.storagehub.service.TaskService;
import com.storagehub.service.payment.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import vn.payos.PayOS;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RentalExtensionPaymentTests {

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
    private ContractService contractService;
    @Mock
    private TaskService taskService;
    @Mock
    private LogService logService;
    @Mock
    private NotificationService notificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PaymentService paymentService;

    private User customer;
    private User staff;
    private Unit unit;
    private Reservation checkedInReservation;

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
        customer = new User("Lan Nguyen", "lan@storagehub.dev", "0901234567", "hash", customerRole, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(customer, "id", 1L);

        Role staffRole = new Role(2, "Staff", "Staff role");
        staff = new User("Minh Tran", "staff@storagehub.dev", "0902345678", "hash", staffRole, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(staff, "id", 2L);

        Facility facility = new Facility("Tan Binh Depot", "45 Nguyen Van Troi", "02839991122", 1);
        ReflectionTestUtils.setField(facility, "id", 1);

        Zone zone = new Zone(facility, "A", 1);
        ReflectionTestUtils.setField(zone, "id", 1);

        UnitType unitType = new UnitType("M", "Medium unit");
        ReflectionTestUtils.setField(unitType, "id", 2);

        unit = new Unit("M-5", unitType, zone, new BigDecimal("8.0"), 1, "QR", UnitStatus.RENTED);
        ReflectionTestUtils.setField(unit, "id", 5L);

        checkedInReservation = new Reservation(
                "BK-2026-0005",
                customer,
                unit,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 1),
                new BigDecimal("207000"),
                "482913",
                ReservationStatus.CHECKED_IN
        );
        checkedInReservation.setMonthlyRate(new BigDecimal("690000"));
        checkedInReservation.setBaseRent(new BigDecimal("2070000"));
        checkedInReservation.setTotalRent(new BigDecimal("2070000"));
        ReflectionTestUtils.setField(checkedInReservation, "id", 50L);
    }

    @Test
    @DisplayName("Story 4.2: Create EXTENSION_FEE payment for non-checked-in reservation throws 409")
    void testCreatePayment_notCheckedIn_throws409() {
        Reservation reserved = new Reservation(
                "BK-2026-0099", customer, unit,
                LocalDate.of(2026, 10, 5), LocalDate.of(2027, 1, 5),
                new BigDecimal("103500"), null, ReservationStatus.RESERVED
        );
        ReflectionTestUtils.setField(reserved, "id", 99L);
        when(reservationRepository.findById(99L)).thenReturn(Optional.of(reserved));

        CreatePaymentRequest request = new CreatePaymentRequest(
                99L,
                PaymentPurpose.EXTENSION_FEE,
                PaymentMethod.PAYOS,
                759000L,
                LocalDate.of(2027, 2, 5)
        );

        assertThatThrownBy(() -> paymentService.createPayment(request, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("expected CHECKED_IN for extension fee payment");
    }

    @Test
    @DisplayName("Story 4.2: Create EXTENSION_FEE payment in Cash sets PENDING_CASH, creates task, and keeps endDate unchanged")
    void testCreatePayment_cash_setsPendingCashAndDoesNotShiftEndDate() {
        when(reservationRepository.findById(50L)).thenReturn(Optional.of(checkedInReservation));
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(reservationRepository.findUpcomingReservationsForUnit(eq(5L), eq(50L), any(), any()))
                .thenReturn(List.of());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 501L);
            return p;
        });

        LocalDate newDate = LocalDate.of(2027, 1, 1);
        CreatePaymentRequest request = new CreatePaymentRequest(
                50L,
                PaymentPurpose.EXTENSION_FEE,
                PaymentMethod.CASH,
                null,
                newDate
        );

        PaymentResponseDto response = paymentService.createPayment(request, 1L);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING_CASH);
        assertThat(response.method()).isEqualTo(PaymentMethod.CASH);
        assertThat(response.purpose()).isEqualTo(PaymentPurpose.EXTENSION_FEE);
        // End date has NOT shifted yet
        assertThat(checkedInReservation.getEndDate()).isEqualTo(LocalDate.of(2026, 12, 1));
        // Verify desk contract signature task was created
        verify(taskService, times(1)).createContractSignatureTask(eq(checkedInReservation), any(), any());
    }

    @Test
    @DisplayName("Story 4.2: Staff confirms cash payment -> shifts endDate, adjusts deposit, drafts addendum CT-...-A1")
    void testConfirmCashPayment_extensionFee_shiftsEndDateAndDraftsAddendum() {
        LocalDate newDate = LocalDate.of(2027, 1, 1); // +31 days
        Payment cashPayment = new Payment(
                "RC-1727932800501",
                customer,
                checkedInReservation,
                null,
                null,
                PaymentPurpose.EXTENSION_FEE,
                PaymentMethod.CASH,
                new BigDecimal("784300"),
                PaymentStatus.PENDING_CASH,
                newDate
        );
        ReflectionTestUtils.setField(cashPayment, "id", 501L);

        when(paymentRepository.findById(501L)).thenReturn(Optional.of(cashPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContractDto mockAddendum = new ContractDto(
                55L,
                "CT-2026-0005-A1",
                50L,
                "BK-2026-0005",
                1,
                "v3",
                "{}",
                null,
                null,
                ContractStatus.AWAITING_SIGNATURE,
                20L,
                1
        );
        when(contractService.createAddendumDraft(eq(checkedInReservation), eq(newDate), any(), any()))
                .thenReturn(mockAddendum);

        PaymentDto result = paymentService.confirmCashPayment(501L, 2L);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(result.method()).isEqualTo(PaymentMethod.CASH);

        // Verify reservation endDate shifted immediately
        assertThat(checkedInReservation.getEndDate()).isEqualTo(newDate);
        // Verify depositAmount was updated (top-up applied)
        assertThat(checkedInReservation.getDepositAmount()).isGreaterThan(new BigDecimal("207000"));
        // Verify baseRent and totalRent increased
        assertThat(checkedInReservation.getTotalRent()).isGreaterThan(new BigDecimal("2070000"));

        // Verify addendum was created
        verify(contractService, times(1)).createAddendumDraft(eq(checkedInReservation), eq(newDate), any(), any());
        // Verify notification sent
        verify(notificationService, times(1)).send(eq(1L), eq("PAYMENT_SUCCEEDED"), any(), any());
    }
}
