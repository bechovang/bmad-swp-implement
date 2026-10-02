package com.storagehub.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storagehub.dto.AttachmentUploadResponseDto;
import com.storagehub.dto.CheckInActivationDto;
import com.storagehub.dto.ContractDto;
import com.storagehub.entity.*;
import com.storagehub.exception.BusinessRuleException;
import com.storagehub.repository.*;
import com.storagehub.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

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
public class ContractRitualTests {

    @Mock
    private ContractRepository contractRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private UnitRepository unitRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PricingEngine pricingEngine;
    @Mock
    private LogService logService;
    @Mock
    private NotificationService notificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ContractService contractService;
    private TaskService taskService;
    private AttachmentService attachmentService;

    private User customer;
    private User staff;
    private Unit unit;
    private RentalPolicy policy;
    private Reservation reservation;
    private Contract contract;
    private Task checkInTask;
    private Payment depositPayment;
    private Payment rentPayment;

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

        taskService = new TaskService(
                taskRepository,
                userRepository,
                reservationRepository,
                unitRepository,
                paymentRepository,
                contractRepository,
                pricingEngine,
                logService,
                notificationService,
                objectMapper
        );

        attachmentService = new AttachmentService();

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

        UnitType unitType = new UnitType("S", "Small unit");
        ReflectionTestUtils.setField(unitType, "id", 1);

        unit = new Unit("S-3", unitType, zone, new BigDecimal("5.0"), 1, "PIN", UnitStatus.AVAILABLE);
        ReflectionTestUtils.setField(unit, "id", 1L);

        policy = new RentalPolicy("v3", LocalDate.of(2026, 1, 1), PolicyStatus.ACTIVE);
        ReflectionTestUtils.setField(policy, "id", 1);

        reservation = new Reservation(
                "BK-1042",
                customer,
                unit,
                LocalDate.of(2026, 10, 3),
                LocalDate.of(2027, 1, 3),
                new BigDecimal("103500"),
                null,
                ReservationStatus.RESERVED
        );
        ReflectionTestUtils.setField(reservation, "id", 100L);

        contract = new Contract(
                "CT-1042",
                reservation,
                policy,
                "{\"code\":\"CT-1042\",\"totalRent\":1035000,\"depositAmount\":103500}",
                null,
                ContractStatus.DRAFT,
                null,
                1
        );
        ReflectionTestUtils.setField(contract, "id", 50L);

        checkInTask = new Task(
                TaskType.CHECK_IN,
                "BK-1042",
                staff,
                LocalDate.of(2026, 10, 3),
                TaskStatus.IN_PROGRESS
        );
        ReflectionTestUtils.setField(checkInTask, "id", 10L);

        depositPayment = new Payment(
                "RC-101",
                customer,
                reservation,
                null,
                null,
                PaymentPurpose.DEPOSIT,
                PaymentMethod.PAYOS,
                new BigDecimal("103500"),
                PaymentStatus.SUCCEEDED
        );
        ReflectionTestUtils.setField(depositPayment, "id", 101L);

        rentPayment = new Payment(
                "RC-102",
                customer,
                reservation,
                null,
                null,
                PaymentPurpose.RENT,
                PaymentMethod.CASH,
                new BigDecimal("1035000"),
                PaymentStatus.SUCCEEDED
        );
        ReflectionTestUtils.setField(rentPayment, "id", 102L);
    }

    @Test
    @DisplayName("signContract transitions DRAFT to SIGNED, records photo URL and logs CONTRACT_SIGNED")
    void signContract_Success() {
        when(contractRepository.findById(50L)).thenReturn(Optional.of(contract));
        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));

        String photoUrl = "/api/v1/attachments/contract-signed-CT-1042-xyz.jpg";
        ContractDto result = contractService.signContract(50L, photoUrl, 2L);

        assertThat(result.status()).isEqualTo(ContractStatus.SIGNED);
        assertThat(result.signedPhotoUrl()).isEqualTo(photoUrl);

        verify(logService).append(
                eq(2L),
                eq(EntityType.CONTRACT),
                eq(50L),
                eq(Action.STATUS_CHANGE),
                eq(ContractStatus.DRAFT.name()),
                eq(ContractStatus.SIGNED.name()),
                contains("Contract signed with photo attachment")
        );

        verify(notificationService).send(
                eq(1L),
                eq("CONTRACT_SIGNED"),
                contains("CT-1042 has been signed"),
                eq("/rentals/100")
        );
    }

    @Test
    @DisplayName("signContract throws error if photo URL is missing")
    void signContract_MissingPhoto() {
        assertThatThrownBy(() -> contractService.signContract(50L, "  ", 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Signed contract photo attachment is required");
    }

    @Test
    @DisplayName("getContractChain returns all revision contracts sorted by ID")
    void getContractChain_Success() {
        Contract superseded = new Contract(
                "CT-1042",
                reservation,
                policy,
                "{}",
                null,
                ContractStatus.SUPERSEDED,
                null,
                0
        );
        ReflectionTestUtils.setField(superseded, "id", 49L);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(contractRepository.findByReservation_Id(100L)).thenReturn(List.of(contract, superseded));

        List<ContractDto> chain = contractService.getContractChain(100L, 1L, false);

        assertThat(chain).hasSize(2);
        assertThat(chain.get(0).id()).isEqualTo(49L);
        assertThat(chain.get(1).id()).isEqualTo(50L);
    }

    @Test
    @DisplayName("activateCheckIn completes in 1 transaction when contract is SIGNED and rent is PAID")
    void activateCheckIn_Success() {
        contract.setStatus(ContractStatus.SIGNED);
        contract.setSignedPhotoUrl("/api/v1/attachments/signed.jpg");

        when(taskRepository.findById(10L)).thenReturn(Optional.of(checkInTask));
        when(reservationRepository.findByCode("BK-1042")).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservationIdOrderByCreatedAtAsc(100L))
                .thenReturn(List.of(depositPayment, rentPayment));
        when(contractRepository.findLatestByReservationId(100L)).thenReturn(Optional.of(contract));

        CheckInActivationDto result = taskService.activateCheckIn(10L, 2L);

        assertThat(result.reservationStatus()).isEqualTo(ReservationStatus.CHECKED_IN);
        assertThat(result.unitStatus()).isEqualTo(UnitStatus.RENTED);
        assertThat(result.taskStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(result.accessCode()).isNotBlank();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CHECKED_IN);
        assertThat(unit.getStatus()).isEqualTo(UnitStatus.RENTED);
        assertThat(checkInTask.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.ACTIVE);

        verify(notificationService).send(
                eq(1L),
                eq("ACCESS_CODE_ISSUED"),
                contains("Access code sent to your notifications"),
                eq("/rentals/100")
        );
    }

    @Test
    @DisplayName("activateCheckIn blocks with CONTRACT_NOT_SIGNED if contract is still DRAFT")
    void activateCheckIn_BlockedWhenContractNotSigned() {
        contract.setStatus(ContractStatus.DRAFT);

        when(taskRepository.findById(10L)).thenReturn(Optional.of(checkInTask));
        when(reservationRepository.findByCode("BK-1042")).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservationIdOrderByCreatedAtAsc(100L))
                .thenReturn(List.of(depositPayment, rentPayment));
        when(contractRepository.findLatestByReservationId(100L)).thenReturn(Optional.of(contract));

        assertThatThrownBy(() -> taskService.activateCheckIn(10L, 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("contract is not signed");
    }

    @Test
    @DisplayName("activateCheckIn blocks with RENT_NOT_PAID if rent has not been paid")
    void activateCheckIn_BlockedWhenRentUnpaid() {
        contract.setStatus(ContractStatus.SIGNED);

        when(taskRepository.findById(10L)).thenReturn(Optional.of(checkInTask));
        when(reservationRepository.findByCode("BK-1042")).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservationIdOrderByCreatedAtAsc(100L))
                .thenReturn(List.of(depositPayment)); // Only deposit, no rent payment

        assertThatThrownBy(() -> taskService.activateCheckIn(10L, 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("100% rent has not been paid");
    }

    @Test
    @DisplayName("AttachmentService stores file safely and rejects unsupported format")
    void attachmentService_UploadValidation() {
        MockMultipartFile validJpg = new MockMultipartFile(
                "file",
                "signed_contract.jpg",
                "image/jpeg",
                "dummy image content".getBytes()
        );

        AttachmentUploadResponseDto uploadRes = attachmentService.storeAttachment(validJpg, 2L);
        assertThat(uploadRes.fileUrl()).startsWith("/api/v1/attachments/");
        assertThat(uploadRes.contentType()).isEqualTo("image/jpeg");

        MockMultipartFile invalidExe = new MockMultipartFile(
                "file",
                "virus.exe",
                "application/x-msdownload",
                "bad bytes".getBytes()
        );

        assertThatThrownBy(() -> attachmentService.storeAttachment(invalidExe, 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only JPEG, PNG, WebP, and PDF files are allowed");
    }

    private static String contains(String s) {
        return org.mockito.ArgumentMatchers.contains(s);
    }
}
