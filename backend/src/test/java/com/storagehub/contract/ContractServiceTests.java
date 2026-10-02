package com.storagehub.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storagehub.dto.ContractDto;
import com.storagehub.dto.PricingBreakdownDto;
import com.storagehub.entity.Action;
import com.storagehub.entity.Contract;
import com.storagehub.entity.ContractStatus;
import com.storagehub.entity.EntityType;
import com.storagehub.entity.Facility;
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
import com.storagehub.repository.ReservationRepository;
import com.storagehub.service.ContractService;
import com.storagehub.service.LogService;
import com.storagehub.service.NotificationService;
import com.storagehub.service.PricingEngine;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ContractServiceTests {

    @Mock
    private ContractRepository contractRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private PricingEngine pricingEngine;
    @Mock
    private LogService logService;
    @Mock
    private NotificationService notificationService;

    private ContractService contractService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private User customer;
    private User otherCustomer;
    private User staff;
    private Unit unit;
    private Reservation reservation;
    private RentalPolicy policy;
    private PricingBreakdownDto pricingBreakdown;

    @BeforeEach
    void setUp() {
        contractService = new ContractService(
                contractRepository,
                reservationRepository,
                pricingEngine,
                logService,
                notificationService,
                objectMapper
        );

        Role customerRole = new Role(1, "Customer", "Customer role");
        Role staffRole = new Role(2, "Staff", "Staff role");

        customer = new User("Lan Nguyen", "lan@storagehub.dev", "0901234567", "hash", customerRole, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(customer, "id", 1L);

        otherCustomer = new User("Minh Tran", "minh@storagehub.dev", "0902345678", "hash", customerRole, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(otherCustomer, "id", 2L);

        staff = new User("Tuấn FM", "tuan@storagehub.dev", "0903456789", "hash", staffRole, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(staff, "id", 3L);

        Facility facility = new Facility("Tan Binh Depot", "45 Nguyen Van Troi", "02839991122", 1);
        Zone zone = new Zone(facility, "A", 1);
        UnitType unitType = new UnitType("S", "Small unit");
        ReflectionTestUtils.setField(unitType, "id", 2);

        unit = new Unit("S-3", unitType, zone, BigDecimal.valueOf(5.0), 1, "PIN", UnitStatus.AVAILABLE);
        ReflectionTestUtils.setField(unit, "id", 10L);

        reservation = new Reservation(
                "BK-2026-0001",
                customer,
                unit,
                LocalDate.of(2026, 10, 3),
                LocalDate.of(2026, 11, 3),
                BigDecimal.valueOf(103500),
                null,
                ReservationStatus.RESERVED
        );
        ReflectionTestUtils.setField(reservation, "id", 100L);

        policy = new RentalPolicy("v3", LocalDate.of(2026, 1, 1), com.storagehub.entity.PolicyStatus.ACTIVE);
        ReflectionTestUtils.setField(policy, "id", 1);

        pricingBreakdown = new PricingBreakdownDto(
                "S-3",
                1,
                BigDecimal.valueOf(345000),
                BigDecimal.valueOf(345000),
                List.of(),
                BigDecimal.valueOf(345000),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(34500),
                true,
                BigDecimal.valueOf(34500),
                "VND",
                "v3"
        );
    }

    @Test
    @DisplayName("createDraftContract creates immutable DRAFT contract with CT- code, locked snapshot, audit log, and notification")
    void testCreateDraftContract_success() {
        when(contractRepository.findLatestByReservationId(100L)).thenReturn(Optional.empty());
        when(pricingEngine.resolveActivePolicy(any(LocalDate.class))).thenReturn(policy);
        when(pricingEngine.calculatePricing(any(Unit.class), anyInt(), any(LocalDate.class))).thenReturn(pricingBreakdown);
        when(contractRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> {
            Contract c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 50L);
            return c;
        });

        ContractDto result = contractService.createDraftContract(reservation);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(50L);
        assertThat(result.code()).isEqualTo("CT-2026-0001");
        assertThat(result.status()).isEqualTo(ContractStatus.DRAFT);
        assertThat(result.isLatest()).isEqualTo(1);
        assertThat(result.policyVersion()).isEqualTo("v3");
        assertThat(result.snapshot()).isNotNull();
        assertThat(result.snapshot().unitCode()).isEqualTo("S-3");
        assertThat(result.snapshot().monthlyRate()).isEqualTo(345000L);
        assertThat(result.snapshot().depositAmount()).isEqualTo(34500L);

        // Verify audit log
        verify(logService).append(
                eq(1L),
                eq(EntityType.CONTRACT),
                eq(50L),
                eq(Action.STATUS_CHANGE),
                eq(null),
                eq(ContractStatus.DRAFT.name()),
                anyString()
        );

        // Verify notification
        verify(notificationService).send(
                eq(1L),
                eq("CONTRACT_DRAFTED"),
                eq("Contract CT-2026-0001 drafted from your booking and Rental Policy v3. You'll sign it at check-in."),
                eq("/rentals/100")
        );
    }

    @Test
    @DisplayName("createDraftContract is idempotent and returns existing contract if already created")
    void testCreateDraftContract_idempotent() {
        Contract existing = new Contract(
                "CT-2026-0001",
                reservation,
                policy,
                "{\"unitCode\":\"S-3\",\"monthlyRate\":345000}",
                null,
                ContractStatus.DRAFT,
                null,
                1
        );
        ReflectionTestUtils.setField(existing, "id", 50L);

        when(contractRepository.findLatestByReservationId(100L)).thenReturn(Optional.of(existing));

        ContractDto result = contractService.createDraftContract(reservation);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(50L);
        assertThat(result.code()).isEqualTo("CT-2026-0001");
        verify(contractRepository, never()).save(any(Contract.class));
        verify(notificationService, never()).send(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("getContract returns contract detail when accessed by owner")
    void testGetContract_byOwner() {
        Contract contract = new Contract(
                "CT-2026-0001",
                reservation,
                policy,
                "{\"unitCode\":\"S-3\",\"monthlyRate\":345000}",
                null,
                ContractStatus.DRAFT,
                null,
                1
        );
        ReflectionTestUtils.setField(contract, "id", 50L);

        when(contractRepository.findById(50L)).thenReturn(Optional.of(contract));

        ContractDto dto = contractService.getContract(50L, 1L, false);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(50L);
        assertThat(dto.code()).isEqualTo("CT-2026-0001");
    }

    @Test
    @DisplayName("getContract throws AccessDeniedException when requested by unauthorized customer")
    void testGetContract_accessDenied() {
        Contract contract = new Contract(
                "CT-2026-0001",
                reservation,
                policy,
                "{\"unitCode\":\"S-3\"}",
                null,
                ContractStatus.DRAFT,
                null,
                1
        );
        ReflectionTestUtils.setField(contract, "id", 50L);

        when(contractRepository.findById(50L)).thenReturn(Optional.of(contract));

        assertThatThrownBy(() -> contractService.getContract(50L, 2L, false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("getLatestContractByReservationId returns latest contract for reservation")
    void testGetLatestContractByReservationId_success() {
        Contract contract = new Contract(
                "CT-2026-0001",
                reservation,
                policy,
                "{\"unitCode\":\"S-3\"}",
                null,
                ContractStatus.DRAFT,
                null,
                1
        );
        ReflectionTestUtils.setField(contract, "id", 50L);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(contractRepository.findLatestByReservationId(100L)).thenReturn(Optional.of(contract));

        ContractDto dto = contractService.getLatestContractByReservationId(100L, 1L, false);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(50L);
        assertThat(dto.reservationId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("reDraftContract supersedes previous draft and creates new draft with revision code and logs")
    void testReDraftContract_success() {
        Contract previousContract = new Contract(
                "CT-2026-0001",
                reservation,
                policy,
                "{\"unitCode\":\"S-3\"}",
                null,
                ContractStatus.DRAFT,
                null,
                1
        );
        ReflectionTestUtils.setField(previousContract, "id", 50L);

        when(contractRepository.findById(50L)).thenReturn(Optional.of(previousContract));
        when(pricingEngine.resolveActivePolicy(any(LocalDate.class))).thenReturn(policy);
        when(pricingEngine.calculatePricing(any(Unit.class), anyInt(), any(LocalDate.class))).thenReturn(pricingBreakdown);
        when(contractRepository.countByReservationId(100L)).thenReturn(1L);
        when(contractRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> {
            Contract c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 51L);
            return c;
        });

        ContractDto newContractDto = contractService.reDraftContract(50L, 3L);

        assertThat(newContractDto).isNotNull();
        assertThat(newContractDto.id()).isEqualTo(51L);
        assertThat(newContractDto.code()).isEqualTo("CT-2026-0001-R1");
        assertThat(newContractDto.status()).isEqualTo(ContractStatus.DRAFT);
        assertThat(newContractDto.isLatest()).isEqualTo(1);
        assertThat(newContractDto.supersedesContractId()).isEqualTo(50L);

        // Verify previous contract was superseded
        assertThat(previousContract.getStatus()).isEqualTo(ContractStatus.SUPERSEDED);
        assertThat(previousContract.getIsLatest()).isEqualTo(0);
        verify(contractRepository).saveAndFlush(previousContract);

        // Verify audit logs for both contracts
        verify(logService).append(
                eq(3L),
                eq(EntityType.CONTRACT),
                eq(50L),
                eq(Action.STATUS_CHANGE),
                eq(ContractStatus.DRAFT.name()),
                eq(ContractStatus.SUPERSEDED.name()),
                anyString()
        );

        verify(logService).append(
                eq(3L),
                eq(EntityType.CONTRACT),
                eq(51L),
                eq(Action.STATUS_CHANGE),
                eq(null),
                eq(ContractStatus.DRAFT.name()),
                anyString()
        );

        // Verify notification
        verify(notificationService).send(
                eq(1L),
                eq("CONTRACT_DRAFTED"),
                eq("Contract CT-2026-0001-R1 drafted from your booking and Rental Policy v3. You'll sign it at check-in."),
                eq("/rentals/100")
        );
    }

    @Test
    @DisplayName("reDraftContract rejects re-drafting if previous contract is not DRAFT")
    void testReDraftContract_invalidStatus() {
        Contract signedContract = new Contract(
                "CT-2026-0001",
                reservation,
                policy,
                "{}",
                null,
                ContractStatus.SIGNED,
                null,
                1
        );
        ReflectionTestUtils.setField(signedContract, "id", 50L);

        when(contractRepository.findById(50L)).thenReturn(Optional.of(signedContract));

        assertThatThrownBy(() -> contractService.reDraftContract(50L, 3L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only DRAFT contracts can be re-drafted");
    }
}
