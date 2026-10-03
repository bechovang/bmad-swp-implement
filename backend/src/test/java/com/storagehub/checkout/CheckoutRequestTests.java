package com.storagehub.checkout;

import com.storagehub.dto.CheckoutRequestDto;
import com.storagehub.dto.CreateCheckoutRequest;
import com.storagehub.entity.*;
import com.storagehub.exception.BusinessRuleException;
import com.storagehub.repository.CheckoutRequestRepository;
import com.storagehub.repository.ReservationRepository;
import com.storagehub.service.CheckoutService;
import com.storagehub.service.LogService;
import com.storagehub.service.NotificationService;
import com.storagehub.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CheckoutRequestTests {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private CheckoutRequestRepository checkoutRequestRepository;
    @Mock
    private TaskService taskService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private LogService logService;

    private CheckoutService checkoutService;

    private User customer;
    private Unit unit;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        checkoutService = new CheckoutService(
                reservationRepository,
                checkoutRequestRepository,
                taskService,
                notificationService,
                logService
        );

        customer = new User("Lan Nguyen", "lan@storagehub.dev", "0901234567", "hash",
                new Role(1, "Customer", null), UserStatus.ACTIVE);
        ReflectionTestUtils.setField(customer, "id", 1L);

        Facility facility = new Facility("Tan Binh Depot", "45 NV Troi", "0900000000", 1);
        Zone zone = new Zone(facility, "A", 1);
        UnitType unitType = new UnitType("Standard", "Standard Lock");

        unit = new Unit("S-3", unitType, zone, BigDecimal.valueOf(5.0), 1, "PIN", UnitStatus.RENTED);
        ReflectionTestUtils.setField(unit, "id", 10L);

        reservation = new Reservation("BK-1042", customer, unit, LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(2),
                BigDecimal.valueOf(200000), "123456", ReservationStatus.CHECKED_IN);
        ReflectionTestUtils.setField(reservation, "id", 100L);
    }

    @Test
    @DisplayName("Submit valid checkout request transitions reservation to CHECKOUT_REQUESTED and spawns task")
    void submitValidCheckoutRequestSucceeds() {
        LocalDate requestedDate = LocalDate.now().plusDays(5);
        CreateCheckoutRequest request = new CreateCheckoutRequest(requestedDate, "Returning keys next week");

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.findUpcomingReservationsForUnit(eq(10L), eq(100L), any(), any()))
                .thenReturn(List.of());
        when(checkoutRequestRepository.findByReservationIdAndStatus(100L, CheckoutRequestStatus.PENDING))
                .thenReturn(List.of());
        when(checkoutRequestRepository.save(any(CheckoutRequest.class))).thenAnswer(invocation -> {
            CheckoutRequest cr = invocation.getArgument(0);
            ReflectionTestUtils.setField(cr, "id", 501L);
            return cr;
        });

        CheckoutRequestDto dto = checkoutService.requestCheckout(100L, request, 1L, "CUSTOMER");

        assertThat(dto).isNotNull();
        assertThat(dto.requestedDate()).isEqualTo(requestedDate);
        assertThat(dto.status()).isEqualTo(CheckoutRequestStatus.PENDING);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CHECKOUT_REQUESTED);

        verify(taskService).createCheckoutTask(eq(reservation), eq(requestedDate));
        verify(notificationService).send(eq(1L), eq("CHECKOUT_REQUESTED"), anyString(), anyString());
    }

    @Test
    @DisplayName("Submitting new checkout request supersedes previous pending checkout request")
    void newRequestSupersedesOlderPendingRequest() {
        LocalDate requestedDate = LocalDate.now().plusDays(7);
        CreateCheckoutRequest request = new CreateCheckoutRequest(requestedDate, "Updated date");

        CheckoutRequest oldPending = new CheckoutRequest(reservation, LocalDate.now().plusDays(3), CheckoutRequestStatus.PENDING, "Old note");
        ReflectionTestUtils.setField(oldPending, "id", 500L);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.findUpcomingReservationsForUnit(eq(10L), eq(100L), any(), any()))
                .thenReturn(List.of());
        when(checkoutRequestRepository.findByReservationIdAndStatus(100L, CheckoutRequestStatus.PENDING))
                .thenReturn(new ArrayList<>(List.of(oldPending)));
        when(checkoutRequestRepository.save(any(CheckoutRequest.class))).thenAnswer(invocation -> {
            CheckoutRequest cr = invocation.getArgument(0);
            ReflectionTestUtils.setField(cr, "id", 502L);
            return cr;
        });

        CheckoutRequestDto dto = checkoutService.requestCheckout(100L, request, 1L, "CUSTOMER");

        assertThat(oldPending.getStatus()).isEqualTo(CheckoutRequestStatus.CANCELLED);
        assertThat(dto.id()).isEqualTo(502L);
    }

    @Test
    @DisplayName("Checkout request date colliding with upcoming reservation is blocked")
    void checkoutDateConflictIsBlocked() {
        LocalDate requestedDate = LocalDate.now().plusDays(20);
        CreateCheckoutRequest request = new CreateCheckoutRequest(requestedDate, "Too late");

        Reservation nextBooking = new Reservation("BK-1043", customer, unit, LocalDate.now().plusDays(15), LocalDate.now().plusMonths(3),
                BigDecimal.valueOf(200000), "654321", ReservationStatus.RESERVED);
        ReflectionTestUtils.setField(nextBooking, "id", 101L);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.findUpcomingReservationsForUnit(eq(10L), eq(100L), any(), any()))
                .thenReturn(List.of(nextBooking));

        assertThatThrownBy(() -> checkoutService.requestCheckout(100L, request, 1L, "CUSTOMER"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(e -> {
                    BusinessRuleException bre = (BusinessRuleException) e;
                    assertThat(bre.getCode()).isEqualTo("CHECKOUT_DATE_CONFLICT");
                });
    }

    @Test
    @DisplayName("Past checkout date is rejected with INVALID_DATE")
    void pastCheckoutDateIsRejected() {
        LocalDate pastDate = LocalDate.now().minusDays(1);
        CreateCheckoutRequest request = new CreateCheckoutRequest(pastDate, "Past date");

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> checkoutService.requestCheckout(100L, request, 1L, "CUSTOMER"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(e -> {
                    BusinessRuleException bre = (BusinessRuleException) e;
                    assertThat(bre.getCode()).isEqualTo("INVALID_DATE");
                });
    }
}
