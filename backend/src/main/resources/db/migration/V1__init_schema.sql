-- =====================================================================
-- StorageHub V1__init_schema - full 22-entity model V3 in ONE migration.
-- Source of truth: ERD_StateChart_Drawio/ERD_StorageHub.dbml (22 tables,
-- 31 relations) + exactly the 7 V1 deltas pinned by AD-6:
--   [D1] All money columns DECIMAL(15,0)  (overrides model V3 DECIMAL(18,2) - VND, no fraction)
--   [D2] contracts: 1-N per reservation via SupersedesContractID + IsLatest
--        (replaces the model's 1:1 UNIQUE(ReservationID); "one original contract"
--        now means "exactly one row with IsLatest = 1", enforced by uk_contracts_latest)
--   [D3] policy_rules.RuleType adds TURNOVER_BUFFER / DISCOUNT / WAIVER_CAP
--   [D4] users.FacilityID nullable
--   [D5] notifications.CreatedAt DEFAULT CURRENT_TIMESTAMP
--   [D6] escalations.TicketID UNIQUE (one escalation per ticket in v1)
--   [D7] inspections.Item enum ACCESS_CARD / PADLOCK / CLEANLINESS / STRUCTURE
-- Unit photos are FE static assets (public/units/{code}.jpg) - no entity, count stays 22.
-- Conventions: timestamps stored as UTC Instant semantics (DATETIME), business
-- dates are DATE (Asia/Ho_Chi_Minh per AD-7). No manual DDL after this (AD-6).
-- Target: MySQL 8.4 LTS, InnoDB, utf8mb4.
-- =====================================================================

-- ---------------------------------------- NHOM 1: NGUOI DUNG & CO SO ----

CREATE TABLE roles (
    RoleID      INT          NOT NULL AUTO_INCREMENT,
    Name        VARCHAR(50)  NOT NULL,
    Description VARCHAR(200) NULL,
    PRIMARY KEY (RoleID),
    UNIQUE KEY uk_roles_name (Name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE facilities (
    FacilityID INT          NOT NULL AUTO_INCREMENT,
    Name       VARCHAR(100) NOT NULL,
    Address    VARCHAR(255) NOT NULL,
    Phone      VARCHAR(20)  NULL,
    Status     TINYINT      NOT NULL DEFAULT 1 COMMENT '0=closed, 1=active',
    PRIMARY KEY (FacilityID)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE zones (
    ZoneID     INT         NOT NULL AUTO_INCREMENT,
    FacilityID INT         NOT NULL,
    Code       VARCHAR(20) NOT NULL,
    Floor      INT         NOT NULL,
    PRIMARY KEY (ZoneID),
    UNIQUE KEY uk_zones_facility_code (FacilityID, Code),
    CONSTRAINT fk_zones_facility FOREIGN KEY (FacilityID) REFERENCES facilities (FacilityID)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE users (
    UserID       BIGINT        NOT NULL AUTO_INCREMENT,
    FullName     VARCHAR(100)  NOT NULL,
    Email        VARCHAR(100)  NOT NULL,
    Phone        VARCHAR(20)   NULL,
    PasswordHash VARCHAR(255)  NOT NULL,
    RoleID       INT           NOT NULL,
    Status       TINYINT       NOT NULL DEFAULT 1 COMMENT '0=inactive, 1=active, 2=locked',
    CreatedAt    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- [D4] nullable facility scope (demo runs 1 facility; multi-facility FM ready)
    FacilityID   INT           NULL,
    PRIMARY KEY (UserID),
    UNIQUE KEY uk_users_email (Email),
    CONSTRAINT fk_users_role FOREIGN KEY (RoleID) REFERENCES roles (RoleID),
    CONSTRAINT fk_users_facility FOREIGN KEY (FacilityID) REFERENCES facilities (FacilityID)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE staff_assignments (
    AssignmentID BIGINT                            NOT NULL AUTO_INCREMENT,
    StaffID      BIGINT                            NOT NULL,
    ZoneID       INT                               NOT NULL,
    Shift        ENUM ('MORNING', 'AFTERNOON', 'EVENING') NOT NULL,
    WorkDate     DATE                              NOT NULL,
    PRIMARY KEY (AssignmentID),
    UNIQUE KEY uk_staff_assignments_slot (StaffID, WorkDate, Shift) COMMENT 'guard against overlapping shift assignment',
    CONSTRAINT fk_staff_assignments_staff FOREIGN KEY (StaffID) REFERENCES users (UserID),
    CONSTRAINT fk_staff_assignments_zone FOREIGN KEY (ZoneID) REFERENCES zones (ZoneID)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- -------------------------------- NHOM 2: KHO & DON VI LUU TRU ----

CREATE TABLE unit_types (
    TypeID      INT          NOT NULL AUTO_INCREMENT,
    Name        VARCHAR(50)  NOT NULL,
    Description VARCHAR(200) NULL,
    PRIMARY KEY (TypeID),
    UNIQUE KEY uk_unit_types_name (Name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE units (
    UnitID       BIGINT       NOT NULL AUTO_INCREMENT,
    Code         VARCHAR(20)  NOT NULL,
    TypeID       INT          NOT NULL,
    ZoneID       INT          NOT NULL,
    SizeM2       DECIMAL(6, 2) NOT NULL COMMENT 'size, not money - keeps 2 decimals',
    Floor        INT          NOT NULL,
    AccessType   VARCHAR(20)  NOT NULL COMMENT 'PIN / QR / smart lock',
    Status       ENUM ('AVAILABLE', 'RESERVED', 'RENTED', 'PREPARING', 'MAINTENANCE', 'RETIRED') NOT NULL,
    MergedIntoID BIGINT       NULL COMMENT 'merge of adjacent units: old unit RETIRED, points at the new one',
    PRIMARY KEY (UnitID),
    UNIQUE KEY uk_units_code (Code),
    CONSTRAINT fk_units_type FOREIGN KEY (TypeID) REFERENCES unit_types (TypeID),
    CONSTRAINT fk_units_zone FOREIGN KEY (ZoneID) REFERENCES zones (ZoneID),
    CONSTRAINT fk_units_merged_into FOREIGN KEY (MergedIntoID) REFERENCES units (UnitID)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- -------------------------------------------- NHOM 3: CHINH SACH GIA ----

CREATE TABLE rental_policies (
    PolicyID      INT         NOT NULL AUTO_INCREMENT,
    Version       VARCHAR(20) NOT NULL,
    EffectiveDate DATE        NOT NULL,
    Status        TINYINT     NOT NULL DEFAULT 0 COMMENT '0=draft, 1=active, 2=retired',
    PRIMARY KEY (PolicyID),
    UNIQUE KEY uk_rental_policies_version (Version)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE policy_rules (
    RuleID         BIGINT       NOT NULL AUTO_INCREMENT,
    PolicyID       INT          NOT NULL,
    TypeID         INT          NOT NULL,
    -- [D3] enum extended with TURNOVER_BUFFER / DISCOUNT / WAIVER_CAP
    RuleType       ENUM ('DEPOSIT_RATE', 'RENT_RATE', 'LATE_FEE', 'SURCHARGE',
                        'TURNOVER_BUFFER', 'DISCOUNT', 'WAIVER_CAP') NOT NULL,
    SurchargeType  ENUM ('PERCENT', 'FIXED') NULL,
    -- [D1] money columns DECIMAL(15,0); rate rules store whole percent (10 = 10%)
    Value          DECIMAL(15, 0) NOT NULL,
    Cap            DECIMAL(15, 0) NULL,
    PRIMARY KEY (RuleID),
    CONSTRAINT fk_policy_rules_policy FOREIGN KEY (PolicyID) REFERENCES rental_policies (PolicyID),
    CONSTRAINT fk_policy_rules_type FOREIGN KEY (TypeID) REFERENCES unit_types (TypeID)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------- NHOM 4: DAT CHO & THUE (BK-) ----

CREATE TABLE reservations (
    ReservationID BIGINT        NOT NULL AUTO_INCREMENT,
    Code          VARCHAR(20)   NOT NULL COMMENT 'BK-2026-0001, unique across the whole book->rent->return lifecycle',
    CustomerID    BIGINT        NOT NULL,
    UnitID        BIGINT        NOT NULL,
    StartDate     DATE          NOT NULL COMMENT 'already includes turnover buffer',
    EndDate       DATE          NOT NULL COMMENT 'moves forward once the extension fee is paid',
    -- [D1]
    DepositAmount DECIMAL(15, 0) NOT NULL COMMENT '10% deposit held for the whole session, can grow via top-up',
    AccessCode    VARCHAR(10)   NULL COMMENT 'issued at check-in (CHECKED_IN = renting)',
    Status        ENUM ('PENDING_PAYMENT', 'RESERVED', 'CHECKED_IN', 'CHECKOUT_REQUESTED',
                        'CLOSED', 'EXPIRED', 'CANCELLED') NOT NULL COMMENT 'CANCELLED reserved by policy - not used in v1',
    PRIMARY KEY (ReservationID),
    UNIQUE KEY uk_reservations_code (Code),
    CONSTRAINT fk_reservations_customer FOREIGN KEY (CustomerID) REFERENCES users (UserID),
    CONSTRAINT fk_reservations_unit FOREIGN KEY (UnitID) REFERENCES units (UnitID)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE extensions (
    ExtensionID   BIGINT        NOT NULL AUTO_INCREMENT,
    ReservationID BIGINT        NOT NULL,
    OldEndDate    DATE          NOT NULL,
    NewEndDate    DATE          NOT NULL,
    -- [D1]
    ExtensionFee  DECIMAL(15, 0) NOT NULL,
    Status        ENUM ('PENDING_PAYMENT', 'APPLIED') NOT NULL,
    PRIMARY KEY (ExtensionID),
    CONSTRAINT fk_extensions_reservation FOREIGN KEY (ReservationID) REFERENCES reservations (ReservationID)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE checkout_requests (
    RequestID     BIGINT     NOT NULL AUTO_INCREMENT,
    ReservationID BIGINT     NOT NULL,
    RequestedDate DATE       NOT NULL COMMENT 'latest request wins so the next reservation is never overridden',
    Status        ENUM ('PENDING', 'DONE') NOT NULL,
    PRIMARY KEY (RequestID),
    CONSTRAINT fk_checkout_requests_reservation FOREIGN KEY (ReservationID) REFERENCES reservations (ReservationID)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- --------------------- NHOM 5: HOP DONG & PHU LUC (CT-/CT-...-A1) ----

CREATE TABLE contracts (
    ContractID           BIGINT        NOT NULL AUTO_INCREMENT,
    Code                 VARCHAR(20)   NOT NULL COMMENT 'CT-2026-0001',
    -- [D2] 1-N per reservation: re-draft keeps the superseded chain readable.
    ReservationID        BIGINT        NOT NULL,
    PolicyID             INT           NOT NULL COMMENT 'policy version locked at signing (snapshot)',
    ContentSnapshot      TEXT          NOT NULL,
    SignedPhotoUrl       VARCHAR(255)  NULL COMMENT 'relative API path /api/v1/attachments/{id} (AD-10)',
    Status               ENUM ('DRAFT', 'PRINTED', 'SIGNED', 'ACTIVE', 'CLOSED', 'SUPERSEDED') NOT NULL,
    -- [D2] chain + latest flag
    SupersedesContractID BIGINT        NULL,
    IsLatest             TINYINT       NOT NULL DEFAULT 1 COMMENT '1 = the one live version of this reservation',
    -- [D2] generated column turns "at most one IsLatest=1 per reservation" into a
    -- real unique constraint (MySQL ignores NULLs in unique keys).
    LatestReservationID  BIGINT        GENERATED ALWAYS AS (CASE WHEN IsLatest = 1 THEN ReservationID END) STORED,
    PRIMARY KEY (ContractID),
    UNIQUE KEY uk_contracts_code (Code),
    UNIQUE KEY uk_contracts_latest (LatestReservationID),
    CONSTRAINT fk_contracts_reservation FOREIGN KEY (ReservationID) REFERENCES reservations (ReservationID),
    CONSTRAINT fk_contracts_policy FOREIGN KEY (PolicyID) REFERENCES rental_policies (PolicyID),
    CONSTRAINT fk_contracts_supersedes FOREIGN KEY (SupersedesContractID) REFERENCES contracts (ContractID),
    CONSTRAINT ck_contracts_is_latest CHECK (IsLatest IN (0, 1))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE contract_addendums (
    AddendumID       BIGINT       NOT NULL AUTO_INCREMENT,
    ContractID       BIGINT       NOT NULL COMMENT 'parent original contract',
    ExtensionID      BIGINT       NULL COMMENT '1:N - a VOIDED rewrite is a new row on the same extension',
    ContentSnapshot  TEXT         NOT NULL,
    SignedPhotoUrl   VARCHAR(255) NULL,
    SignatureDueDate DATE         NOT NULL COMMENT '7-day desk deadline',
    Status           ENUM ('AWAITING_SIGNATURE', 'SIGNED', 'EXPIRED', 'VOIDED') NOT NULL,
    PRIMARY KEY (AddendumID),
    CONSTRAINT fk_contract_addendums_contract FOREIGN KEY (ContractID) REFERENCES contracts (ContractID),
    CONSTRAINT fk_contract_addendums_extension FOREIGN KEY (ExtensionID) REFERENCES extensions (ExtensionID)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ------------------------------- NHOM 6: THANH TOAN & THANH LY ----
-- settlements before payments: payments.SettlementID references settlements.

CREATE TABLE settlements (
    SettlementID  BIGINT        NOT NULL AUTO_INCREMENT,
    ReservationID BIGINT        NOT NULL COMMENT '1:0..1 - settled exactly once per session',
    ContractID    BIGINT        NOT NULL,
    StaffID       BIGINT        NOT NULL,
    -- [D1]
    DamageFee     DECIMAL(15, 0) NOT NULL DEFAULT 0,
    DamageReason  VARCHAR(255)  NULL COMMENT 'required the moment DamageFee > 0',
    RefundAmount  DECIMAL(15, 0) NOT NULL COMMENT 'held deposit minus damage fee',
    ReceiptCode   VARCHAR(20)   NOT NULL,
    PRIMARY KEY (SettlementID),
    UNIQUE KEY uk_settlements_reservation (ReservationID),
    UNIQUE KEY uk_settlements_receipt (ReceiptCode),
    CONSTRAINT fk_settlements_reservation FOREIGN KEY (ReservationID) REFERENCES reservations (ReservationID),
    CONSTRAINT fk_settlements_contract FOREIGN KEY (ContractID) REFERENCES contracts (ContractID),
    CONSTRAINT fk_settlements_staff FOREIGN KEY (StaffID) REFERENCES users (UserID),
    CONSTRAINT ck_settlements_damage_reason CHECK (DamageFee = 0 OR DamageReason IS NOT NULL),
    CONSTRAINT ck_settlements_non_negative CHECK (DamageFee >= 0 AND RefundAmount >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE payments (
    PaymentID     BIGINT        NOT NULL AUTO_INCREMENT,
    ReceiptCode   VARCHAR(20)   NOT NULL,
    PayerID       BIGINT        NOT NULL,
    ReservationID BIGINT        NULL COMMENT '10% deposit + 100% rent share this reference',
    ExtensionID   BIGINT        NULL COMMENT 'extension fee',
    SettlementID  BIGINT        NULL COMMENT 'damage / extra fees beyond the deposit',
    Purpose       ENUM ('DEPOSIT', 'RENT', 'EXTENSION_FEE', 'DAMAGE_FEE', 'EXTRA_FEE') NOT NULL,
    Method        ENUM ('CARD', 'MOMO', 'VNPAY') NOT NULL,
    -- [D1]
    Amount        DECIMAL(15, 0) NOT NULL,
    Status        ENUM ('PENDING', 'PROCESSING', 'SUCCEEDED', 'FAILED', 'EXPIRED') NOT NULL,
    PRIMARY KEY (PaymentID),
    UNIQUE KEY uk_payments_receipt (ReceiptCode),
    CONSTRAINT fk_payments_payer FOREIGN KEY (PayerID) REFERENCES users (UserID),
    CONSTRAINT fk_payments_reservation FOREIGN KEY (ReservationID) REFERENCES reservations (ReservationID),
    CONSTRAINT fk_payments_extension FOREIGN KEY (ExtensionID) REFERENCES extensions (ExtensionID),
    CONSTRAINT fk_payments_settlement FOREIGN KEY (SettlementID) REFERENCES settlements (SettlementID),
    CONSTRAINT ck_payments_exactly_one_ref CHECK (
        (CASE WHEN ReservationID IS NOT NULL THEN 1 ELSE 0 END +
         CASE WHEN ExtensionID IS NOT NULL THEN 1 ELSE 0 END +
         CASE WHEN SettlementID IS NOT NULL THEN 1 ELSE 0 END) = 1
    ),
    CONSTRAINT ck_payments_amount_positive CHECK (Amount > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE inspections (
    InspectionID BIGINT       NOT NULL AUTO_INCREMENT,
    SettlementID BIGINT       NOT NULL,
    -- [D7]
    Item         ENUM ('ACCESS_CARD', 'PADLOCK', 'CLEANLINESS', 'STRUCTURE') NOT NULL,
    Result       ENUM ('OK', 'MINOR', 'MAJOR') NOT NULL COMMENT 'MAJOR is the basis for a damage fee',
    Note         VARCHAR(255) NULL,
    PRIMARY KEY (InspectionID),
    CONSTRAINT fk_inspections_settlement FOREIGN KEY (SettlementID) REFERENCES settlements (SettlementID)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ------------------------------------------------- NHOM 7: HO TRO ----

CREATE TABLE support_tickets (
    TicketID         BIGINT       NOT NULL AUTO_INCREMENT,
    Code             VARCHAR(20)  NOT NULL COMMENT 'SR-2026-0001',
    CustomerID       BIGINT       NOT NULL,
    UnitID           BIGINT       NOT NULL,
    ReservationID    BIGINT       NULL COMMENT 'optional session context',
    IncidentType     ENUM ('LOST_ACCESS', 'DEVICE_ISSUE', 'SECURITY', 'CLEANLINESS', 'OTHER') NOT NULL,
    Status           ENUM ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'ESCALATED') NOT NULL,
    AssignedStaffID  BIGINT       NULL COMMENT 'routed to the on-shift staff of the unit zone',
    PRIMARY KEY (TicketID),
    UNIQUE KEY uk_support_tickets_code (Code),
    CONSTRAINT fk_support_tickets_customer FOREIGN KEY (CustomerID) REFERENCES users (UserID),
    CONSTRAINT fk_support_tickets_unit FOREIGN KEY (UnitID) REFERENCES units (UnitID),
    CONSTRAINT fk_support_tickets_reservation FOREIGN KEY (ReservationID) REFERENCES reservations (ReservationID),
    CONSTRAINT fk_support_tickets_staff FOREIGN KEY (AssignedStaffID) REFERENCES users (UserID)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE escalations (
    EscalationID       BIGINT       NOT NULL AUTO_INCREMENT,
    -- [D6] v1 convention: each ticket escalates at most once
    TicketID           BIGINT       NOT NULL,
    EscalatedByStaffID BIGINT       NOT NULL,
    ManagerID          BIGINT       NULL,
    Note               TEXT         NOT NULL COMMENT 'staff note is mandatory when escalating',
    Decision           ENUM ('PENDING', 'MAINTENANCE_RELOCATE', 'RETURN_TO_STAFF') NOT NULL DEFAULT 'PENDING',
    PRIMARY KEY (EscalationID),
    UNIQUE KEY uk_escalations_ticket (TicketID),
    CONSTRAINT fk_escalations_ticket FOREIGN KEY (TicketID) REFERENCES support_tickets (TicketID),
    CONSTRAINT fk_escalations_staff FOREIGN KEY (EscalatedByStaffID) REFERENCES users (UserID),
    CONSTRAINT fk_escalations_manager FOREIGN KEY (ManagerID) REFERENCES users (UserID)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ------------------------------ NHOM 8: VAN HANH & HE THONG ----

CREATE TABLE tasks (
    TaskID           BIGINT       NOT NULL AUTO_INCREMENT,
    Type             ENUM ('CHECK_IN', 'CHECKOUT', 'CLEANING', 'SUPPORT', 'CONTRACT') NOT NULL,
    RefCode          VARCHAR(20)  NULL COMMENT 'BK- / SR- / CT- code of the object the task works on',
    AssignedStaffID  BIGINT       NOT NULL,
    WorkDate         DATE         NOT NULL,
    Status           ENUM ('TODO', 'IN_PROGRESS', 'DONE') NOT NULL,
    PRIMARY KEY (TaskID),
    CONSTRAINT fk_tasks_staff FOREIGN KEY (AssignedStaffID) REFERENCES users (UserID)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE notifications (
    NotificationID BIGINT       NOT NULL AUTO_INCREMENT,
    UserID         BIGINT       NOT NULL,
    Type           VARCHAR(50)  NOT NULL,
    Title          VARCHAR(200) NOT NULL,
    DeepLink       VARCHAR(255) NULL COMMENT 'relative FE path from contracts/routes.yaml, e.g. /rentals/{id}',
    IsRead         TINYINT      NOT NULL DEFAULT 0,
    -- [D5]
    CreatedAt      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (NotificationID),
    CONSTRAINT fk_notifications_user FOREIGN KEY (UserID) REFERENCES users (UserID)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE activity_logs (
    LogID      BIGINT       NOT NULL AUTO_INCREMENT,
    ActorID    BIGINT       NOT NULL,
    EntityType VARCHAR(50)  NOT NULL COMMENT 'UNIT, CONTRACT, ... shared enum registry (story 1.2)',
    EntityID   BIGINT       NOT NULL,
    Action     VARCHAR(50)  NOT NULL COMMENT 'STATUS_CHANGE, LOGIN, ... shared enum registry (story 1.2)',
    FromValue  VARCHAR(255) NULL,
    ToValue    VARCHAR(255) NULL,
    Reason     VARCHAR(255) NOT NULL,
    PRIMARY KEY (LogID),
    CONSTRAINT fk_activity_logs_actor FOREIGN KEY (ActorID) REFERENCES users (UserID)
    -- Append-only by construction: no UPDATE/DELETE path exists (LogService, story 1.2).
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
