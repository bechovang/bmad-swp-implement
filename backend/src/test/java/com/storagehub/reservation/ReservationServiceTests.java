package com.storagehub.reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storagehub.dto.CreateReservationRequest;
import com.storagehub.dto.PricingBreakdownDto;
import com.storagehub.dto.ReservationDto;
import com.storagehub.entity.Action;
import com.storagehub.entity.Contract;
import com.storagehub.entity.ContractStatus;
import com.storagehub.entity.EntityType;
import com.storagehub.entity.Facility;
import com.storagehub.entity.PolicyRule;
import com.storagehub.entity.PolicyRuleType;
import com.storagehub.entity.PolicyStatus;
import com.storagehub.entity.RentalPolicy;
import com.storagehub.entity.Reservation;
import com.storagehub.entity.ReservationStatus;
import com.storagehub.entity.Role;
import com.storagehub.entity.Unit;
import com.storagehub.entity.UnitStatus;
import com.storagehub.entity.UnitType;
import com.storagehub.entity.User;
import com.storagehub.entity.UserStatus;
import com.storagehub.entity.Zone;
import com.storagehub.exception.BusinessRuleException;
import com.storagehub.exception.ResourceNotFoundException;
import com.storagehub.repository.ContractRepository;
import com.storagehub.repository.PolicyRuleRepository;
import com.storagehub.repository.ReservationRepository;
import com.storagehub.repository.UnitRepository;
import com.storagehub.repository.UserRepository;
import com.storagehub.service.LogService;
import com.storagehub.service.PricingEngine;
import com.storagehub.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTests {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private PolicyRuleRepository policyRuleRepository;

    @Mock
    private PricingEngine pricingEngine;

    @Mock
    private LogService logService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ReservationService reservationService;

    private User testCustomer;
    private Unit testUnit;
    private UnitType testUnitType;
    private RentalPolicy activePolicy;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(
                reservationRepository,
                unitRepository,
                userRepository,
                contractRepository,
                policyRuleRepository,
                pricingEngine,
                logService,
                objectMapper
        );

        Role customerRole = new Role(1, "Customer", null);
        testCustomer = new User("Lan Nguyen", "lan@storagehub.dev", "0901234567", "hash", customerRole, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(testCustomer, "id", 1L);

        testUnitType = new UnitType(1, "S", "Small unit around 5 m2");
        Facility facility = new Facility("Tan Binh Depot", "45 Nguyen Van Troi", "02839991122", 1);
        Zone zone = new Zone(facility, "A", 1);
        testUnit = new Unit(1L, "S-3", testUnitType, zone, new BigDecimal("5.00"), 1, "PIN", UnitStatus.AVAILABLE);

        activePolicy = new RentalPolicy("v3", LocalDate.now().minusDays(10), PolicyStatus.ACTIVE);
    }

    @Test
    @DisplayName("createReservation: succeeds, saves reservation PENDING_PAYMENT, drafts contract snapshot, and logs audit")
    void createReservation_success() {
        CreateReservationRequest request = new CreateReservationRequest("S-3", LocalDate.of(2026, 10, 5), 3);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(unitRepository.findByCodeWithDetails("S-3")).thenReturn(Optional.of(testUnit));
        when(pricingEngine.resolveActivePolicy(request.startDate())).thenReturn(activePolicy);
        when(reservationRepository.findConflictingReservations(any(), any(), any(), any())).thenReturn(List.of());

        PricingBreakdownDto pricing = new PricingBreakdownDto(
                "S-3", 3, new BigDecimal("345000"), new BigDecimal("1035000"),
                List.of(), new BigDecimal("1035000"), new BigDecimal("10"),
                new BigDecimal("103500"), true, new BigDecimal("103500"), "VND", "v3"
        );
        when(pricingEngine.calculatePricing(testUnit, 3, request.startDate())).thenReturn(pricing);

        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            ReflectionTestUtils.setField(r, "id", 100L);
            return r;
        });

        ReservationDto result = reservationService.createReservation(request, 1L);

        assertThat(result).isNotNull();
        assertThat(result.unitCode()).isEqualTo("S-3");
        assertThat(result.durationMonths()).isEqualTo(3);
        assertThat(result.depositAmount()).isEqualTo(103500L);
        assertThat(result.monthlyRate()).isEqualTo(345000L);
        assertThat(result.baseRent()).isEqualTo(1035000L);
        assertThat(result.totalRent()).isEqualTo(1035000L);
        assertThat(result.status()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
        assertThat(result.code()).startsWith("BK-");

        ArgumentCaptor<Contract> contractCaptor = ArgumentCaptor.forClass(Contract.class);
        verify(contractRepository).save(contractCaptor.capture());
        Contract savedContract = contractCaptor.getValue();
        assertThat(savedContract.getStatus()).isEqualTo(ContractStatus.DRAFT);
        assertThat(savedContract.getContentSnapshot()).contains("\"monthlyRate\":345000");

        verify(logService).append(eq(testCustomer.getId()), eq(EntityType.RESERVATION), any(),
                eq(Action.STATUS_CHANGE), any(), eq(ReservationStatus.PENDING_PAYMENT.name()), eq("Reservation created"));
    }

    @Test
    @DisplayName("createReservation: unit not found throws ResourceNotFoundException")
    void createReservation_unitNotFound() {
        CreateReservationRequest request = new CreateReservationRequest("NONEXISTENT", LocalDate.of(2026, 10, 5), 1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(unitRepository.findByCodeWithDetails("NONEXISTENT")).thenReturn(Optional.empty());
        when(unitRepository.findByCode("NONEXISTENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("NONEXISTENT");
    }

    @Test
    @DisplayName("createReservation: unit under MAINTENANCE throws 409 UNIT_UNAVAILABLE")
    void createReservation_unitMaintenance_throws409() {
        testUnit = new Unit(1L, "S-3", testUnitType, testUnit.getZone(), new BigDecimal("5.00"), 1, "PIN", UnitStatus.MAINTENANCE);
        CreateReservationRequest request = new CreateReservationRequest("S-3", LocalDate.of(2026, 10, 5), 1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(unitRepository.findByCodeWithDetails("S-3")).thenReturn(Optional.of(testUnit));

        assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> {
                    BusinessRuleException bre = (BusinessRuleException) ex;
                    assertThat(bre.getCode()).isEqualTo("UNIT_UNAVAILABLE");
                    assertThat(bre.getMessage()).contains("maintenance");
                });
    }

    @Test
    @DisplayName("createReservation: conflicting active reservation throws 409 UNIT_UNAVAILABLE")
    void createReservation_conflict_throws409() {
        CreateReservationRequest request = new CreateReservationRequest("S-3", LocalDate.of(2026, 10, 5), 1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(unitRepository.findByCodeWithDetails("S-3")).thenReturn(Optional.of(testUnit));
        when(pricingEngine.resolveActivePolicy(request.startDate())).thenReturn(activePolicy);

        Reservation conflict = new Reservation("BK-2026-9999", testCustomer, testUnit,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1),
                new BigDecimal("100000"), null, ReservationStatus.RESERVED);
        when(reservationRepository.findConflictingReservations(any(), any(), any(), any()))
                .thenReturn(List.of(conflict));

        assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> {
                    BusinessRuleException bre = (BusinessRuleException) ex;
                    assertThat(bre.getCode()).isEqualTo("UNIT_UNAVAILABLE");
                });
    }

    @Test
    @DisplayName("createReservation: PREPARING unit in cleaning buffer past start date throws 409 UNIT_UNAVAILABLE")
    void createReservation_cleaningBufferConflict_throws409() {
        testUnit = new Unit(1L, "S-3", testUnitType, testUnit.getZone(), new BigDecimal("5.00"), 1, "PIN", UnitStatus.PREPARING);
        CreateReservationRequest request = new CreateReservationRequest("S-3", LocalDate.now().plusDays(1), 1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(unitRepository.findByCodeWithDetails("S-3")).thenReturn(Optional.of(testUnit));
        when(pricingEngine.resolveActivePolicy(request.startDate())).thenReturn(activePolicy);

        PolicyRule bufferRule = new PolicyRule(activePolicy, testUnitType, PolicyRuleType.TURNOVER_BUFFER, null, new BigDecimal("3"), null);
        when(policyRuleRepository.findByPolicy_IdAndUnitType_IdAndRuleType(any(), any(), eq(PolicyRuleType.TURNOVER_BUFFER)))
                .thenReturn(Optional.of(bufferRule));

        when(reservationRepository.findByUnitIdOrderByEndDateDesc(any())).thenReturn(List.of());

        assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> {
                    BusinessRuleException bre = (BusinessRuleException) ex;
                    assertThat(bre.getCode()).isEqualTo("UNIT_UNAVAILABLE");
                    assertThat(bre.getMessage()).contains("cleaning buffer");
                });
    }

    @Test
    @DisplayName("getReservation: customer accessing own reservation succeeds with snapshot")
    void getReservation_customerOwnReservation_success() {
        Reservation reservation = new Reservation("BK-2026-0001", testCustomer, testUnit,
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 11, 5),
                new BigDecimal("103500"), null, ReservationStatus.PENDING_PAYMENT);
        ReflectionTestUtils.setField(reservation, "id", 10L);

        Contract contract = new Contract("CT-2026-0001", reservation, activePolicy,
                "{\"monthlyRate\":345000,\"baseRent\":345000,\"totalRent\":345000,\"depositAmount\":103500,\"durationMonths\":1,\"policyVersion\":\"v3\"}",
                null, ContractStatus.DRAFT, null, 1);

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));
        when(contractRepository.findLatestByReservationId(any())).thenReturn(Optional.of(contract));

        ReservationDto dto = reservationService.getReservation(10L, testCustomer.getId(), false);

        assertThat(dto).isNotNull();
        assertThat(dto.code()).isEqualTo("BK-2026-0001");
        assertThat(dto.monthlyRate()).isEqualTo(345000L);
        assertThat(dto.policyVersion()).isEqualTo("v3");
    }

    @Test
    @DisplayName("getReservation: non-owner non-staff customer gets AccessDeniedException")
    void getReservation_otherCustomer_denied() {
        Reservation reservation = new Reservation("BK-2026-0001", testCustomer, testUnit,
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 11, 5),
                new BigDecimal("103500"), null, ReservationStatus.PENDING_PAYMENT);
        ReflectionTestUtils.setField(reservation, "id", 10L);

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.getReservation(10L, 999L, false))
                .isInstanceOf(AccessDeniedException.class);
    }
}
