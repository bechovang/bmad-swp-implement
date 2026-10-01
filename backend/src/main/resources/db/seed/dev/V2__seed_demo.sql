-- =====================================================================
-- StorageHub V2__seed_demo - the standard demo dataset (dev profile only).
-- Loaded exclusively through the Flyway location classpath:db/seed/dev which
-- only profile "dev" declares - reproducible from zero, no manual inserts.
--
-- Standard mock set (PRD 7.1 / AD-6):
--   users    : Lan (Customer) / Minh (Staff) / Tuan (Facility Manager)
--              / Hang (Business Ops) / Nam (System Administrator)
--   units    : S-3 / M-2 / M-5
--   chain    : BK-1042 (reservation) -> CT-1042 (contract) -> CT-1042-A1
--              (addendum), RT-0871 (checkout task), SR-0032 (support ticket)
--   policy   : v3 (active; v1/v2 kept as history)
--
-- Snapshot moment: 2026-10-19, right after the UJ-1 climax - Lan's rental is
-- closed with a transparent settlement, S-3 is in turnover cleaning, the M-2
-- flooding ticket sits escalated and undecided, M-5 is bookable.
--
-- Demo sign-in (all five accounts): password = Demo1234!
--   lan@storagehub.dev  minh@storagehub.dev  tuan@storagehub.dev
--   hang@storagehub.dev nam@storagehub.dev
-- (BCrypt cost 10 - Spring Security verifies $2a$ hashes.)
--
-- Conventions: money columns are whole VND (DECIMAL(15,0)); timestamps are
-- UTC; addendum codes are derived (parent code + "-A" + seq) and mirrored
-- inside ContentSnapshot because the schema has no Code column on
-- contract_addendums (model V3 is the truth).
-- =====================================================================

-- ---------------------------------------------- roles & people ----

INSERT INTO roles (RoleID, Name, Description) VALUES
    (1, 'Customer', 'Self-service tenant - books, pays, occupies, returns'),
    (2, 'Staff', 'Desk and floor operations - task board'),
    (3, 'Facility Manager', 'Facility oversight - units, staffing, escalations'),
    (4, 'Business Ops', 'Pricing policy and reporting'),
    (5, 'System Administrator', 'Platform administration - users, audit');

INSERT INTO facilities (FacilityID, Name, Address, Phone, Status) VALUES
    (1, 'Tan Binh Depot', '45 Nguyen Van Troi, Tan Binh, Ho Chi Minh City', '02839991122', 1);

INSERT INTO zones (ZoneID, FacilityID, Code, Floor) VALUES
    (1, 1, 'A', 1),
    (2, 1, 'B', 1),
    (3, 1, 'C', 2);

INSERT INTO users (UserID, FullName, Email, Phone, PasswordHash, RoleID, Status, CreatedAt, FacilityID) VALUES
    (1, 'Lan Nguyen',    'lan@storagehub.dev',  '0901234567', '$2a$10$kB0vQAnKi.enlB8C.EgxR.jSPrdJiQcN9ODdOz.OCRFNIFDB/.aqu', 1, 1, '2026-09-28 03:00:00', NULL),
    (2, 'Minh Tran',     'minh@storagehub.dev', '0902345678', '$2a$10$kB0vQAnKi.enlB8C.EgxR.jSPrdJiQcN9ODdOz.OCRFNIFDB/.aqu', 2, 1, '2026-09-01 01:00:00', 1),
    (3, 'Tuan Le',       'tuan@storagehub.dev', '0903456789', '$2a$10$kB0vQAnKi.enlB8C.EgxR.jSPrdJiQcN9ODdOz.OCRFNIFDB/.aqu', 3, 1, '2026-09-01 01:05:00', 1),
    (4, 'Hang Vo',       'hang@storagehub.dev', '0904567890', '$2a$10$kB0vQAnKi.enlB8C.EgxR.jSPrdJiQcN9ODdOz.OCRFNIFDB/.aqu', 4, 1, '2026-09-01 01:10:00', 1),
    (5, 'Nam Pham',      'nam@storagehub.dev',  '0905678901', '$2a$10$kB0vQAnKi.enlB8C.EgxR.jSPrdJiQcN9ODdOz.OCRFNIFDB/.aqu', 5, 1, '2026-09-01 01:15:00', NULL);

-- Minh's shift plan around the story window (guard demo: unique slot).
INSERT INTO staff_assignments (AssignmentID, StaffID, ZoneID, Shift, WorkDate) VALUES
    (1, 2, 1, 'MORNING',   '2026-10-03'),
    (2, 2, 1, 'MORNING',   '2026-10-06'),
    (3, 2, 1, 'MORNING',   '2026-10-15'),
    (4, 2, 1, 'MORNING',   '2026-10-16'),
    (5, 2, 1, 'MORNING',   '2026-10-17'),
    (6, 2, 1, 'MORNING',   '2026-10-18'),
    (7, 2, 1, 'MORNING',   '2026-10-19'),
    (8, 2, 2, 'AFTERNOON', '2026-10-19'),
    (9, 2, 1, 'MORNING',   '2026-10-20');

-- ------------------------------------------------- unit catalog ----

INSERT INTO unit_types (TypeID, Name, Description) VALUES
    (1, 'Locker', '0.5 m2 smart locker, contactless PIN'),
    (2, 'S',      'Small unit around 5 m2'),
    (3, 'M',      'Medium unit around 8 m2'),
    (4, 'L',      'Large unit around 12 m2');

INSERT INTO units (UnitID, Code, TypeID, ZoneID, SizeM2, Floor, AccessType, Status, MergedIntoID) VALUES
    (1, 'S-3', 2, 1, 5.00, 1, 'PIN',       'PREPARING',   NULL)  -- post-checkout cleaning pending (RT-0871 done)
    ,(2, 'M-2', 3, 2, 8.00, 1, 'QR',       'MAINTENANCE', NULL)  -- flooding SR-0033, escalated, decision pending
    ,(3, 'M-5', 3, 2, 8.00, 2, 'QR',       'AVAILABLE',   NULL); -- bookable

-- ------------------------------------------------ pricing policy ----
-- v1/v2 kept as retired history so Policy Management has a version list.

INSERT INTO rental_policies (PolicyID, Version, EffectiveDate, Status) VALUES
    (1, 'v1', '2026-04-01', 2),
    (2, 'v2', '2026-07-01', 2),
    (3, 'v3', '2026-10-01', 1);

-- v3 rule matrix. Value semantics per RuleType:
--   DEPOSIT_RATE    percent of rent (10 = 10%)
--   RENT_RATE       VND per month
--   LATE_FEE        VND per day, charged at settlement
--   SURCHARGE       PERCENT of monthly rent for late checkout, capped by Cap (10 = 10%)
--   TURNOVER_BUFFER hours of sanitization lock between rentals (AD-6 delta)
--   DISCOUNT        percent discount for the unit type (FR-40)
--   WAIVER_CAP      maximum waived VND at settlement (FR-41)
INSERT INTO policy_rules (RuleID, PolicyID, TypeID, RuleType, SurchargeType, Value, Cap) VALUES
    ( 1, 3, 1, 'DEPOSIT_RATE',    NULL,      10,     NULL),
    ( 2, 3, 1, 'RENT_RATE',       NULL,  120000,     NULL),
    ( 3, 3, 1, 'LATE_FEE',        NULL,   30000,     NULL),
    ( 4, 3, 1, 'SURCHARGE',       'PERCENT',  5,     10),
    ( 5, 3, 1, 'TURNOVER_BUFFER', NULL,       2,     NULL),
    ( 6, 3, 1, 'WAIVER_CAP',      NULL,   50000,     NULL),
    ( 7, 3, 2, 'DEPOSIT_RATE',    NULL,      10,     NULL),
    ( 8, 3, 2, 'RENT_RATE',       NULL,  345000,     NULL),
    ( 9, 3, 2, 'LATE_FEE',        NULL,   50000,     NULL),
    (10, 3, 2, 'SURCHARGE',       'PERCENT',  5,     10),
    (11, 3, 2, 'TURNOVER_BUFFER', NULL,       2,     NULL),
    (12, 3, 2, 'DISCOUNT',        NULL,       5,     NULL),
    (13, 3, 2, 'WAIVER_CAP',      NULL,   50000,     NULL),
    (14, 3, 3, 'DEPOSIT_RATE',    NULL,      10,     NULL),
    (15, 3, 3, 'RENT_RATE',       NULL,  690000,     NULL),
    (16, 3, 3, 'LATE_FEE',        NULL,  100000,     NULL),
    (17, 3, 3, 'SURCHARGE',       'PERCENT',  5,     10),
    (18, 3, 3, 'TURNOVER_BUFFER', NULL,       2,     NULL),
    (19, 3, 3, 'DISCOUNT',        NULL,       5,     NULL),
    (20, 3, 3, 'WAIVER_CAP',      NULL,   50000,     NULL),
    (21, 3, 4, 'DEPOSIT_RATE',    NULL,      15,     NULL),
    (22, 3, 4, 'RENT_RATE',       NULL,  980000,     NULL),
    (23, 3, 4, 'LATE_FEE',        NULL,  150000,     NULL),
    (24, 3, 4, 'SURCHARGE',       'PERCENT',  5,     10),
    (25, 3, 4, 'TURNOVER_BUFFER', NULL,       2,     NULL),
    (26, 3, 4, 'DISCOUNT',        NULL,      10,     NULL),
    (27, 3, 4, 'WAIVER_CAP',      NULL,   50000,     NULL);

-- -------------------------------------- the BK-1042 chain (UJ-1) ----
-- Money follows the PRD mock standard exactly:
--   rent 1.035.000 / deposit 103.500 / extension 690.000 / top-up 69.000
--   held deposit 172.500 / damage 40.000 / refund 132.500

INSERT INTO reservations (ReservationID, Code, CustomerID, UnitID, StartDate, EndDate, DepositAmount, AccessCode, Status) VALUES
    (1, 'BK-1042', 1, 1, '2026-10-03', '2026-10-18', 172500, '482913', 'CLOSED');
-- EndDate already reflects the applied extension (was 2026-10-17).

INSERT INTO extensions (ExtensionID, ReservationID, OldEndDate, NewEndDate, ExtensionFee, Status) VALUES
    (1, 1, '2026-10-17', '2026-10-18', 690000, 'APPLIED');

INSERT INTO checkout_requests (RequestID, ReservationID, RequestedDate, Status) VALUES
    (1, 1, '2026-10-18', 'DONE');

INSERT INTO contracts (ContractID, Code, ReservationID, PolicyID, ContentSnapshot, SignedPhotoUrl, Status, SupersedesContractID, IsLatest) VALUES
    (1, 'CT-1042', 1, 3,
     '{"code":"CT-1042","reservation":"BK-1042","unit":"S-3","customer":"Lan Nguyen","startDate":"2026-10-03","endDate":"2026-10-18","rent":1035000,"deposit":103500,"policyVersion":"v3","currency":"VND"}',
     '/api/v1/attachments/1', 'CLOSED', NULL, 1);

INSERT INTO contract_addendums (AddendumID, ContractID, ExtensionID, ContentSnapshot, SignedPhotoUrl, SignatureDueDate, Status) VALUES
    (1, 1, 1,
     '{"code":"CT-1042-A1","contract":"CT-1042","extension":{"oldEndDate":"2026-10-17","newEndDate":"2026-10-18"},"extensionFee":690000,"depositTopUp":69000,"heldDeposit":172500,"currency":"VND"}',
     '/api/v1/attachments/2', '2026-10-22', 'SIGNED');

-- Settlement + inspection checklist (STRUCTURE MINOR -> the 40.000 fee).
INSERT INTO settlements (SettlementID, ReservationID, ContractID, StaffID, DamageFee, DamageReason, RefundAmount, ReceiptCode) VALUES
    (1, 1, 1, 2, 40000, 'Wall scuff on the east panel of unit S-3', 132500, 'RC-2026-0005');

INSERT INTO inspections (InspectionID, SettlementID, Item, Result, Note) VALUES
    (1, 1, 'ACCESS_CARD', 'OK',     NULL),
    (2, 1, 'PADLOCK',     'OK',     NULL),
    (3, 1, 'CLEANLINESS', 'OK',     NULL),
    (4, 1, 'STRUCTURE',   'MINOR',  'Wall scuff on the east panel');

-- Payment trail (each row references exactly one target - CHECK enforced):
-- deposit 103.500 -> rent 1.035.000 -> extension 690.000 -> deposit top-up 69.000.
-- The 40.000 damage fee is deducted inside the settlement, not charged again.
INSERT INTO payments (PaymentID, ReceiptCode, PayerID, ReservationID, ExtensionID, SettlementID, Purpose, Method, Amount, Status) VALUES
    (1, 'RC-2026-0001', 1, 1, NULL, NULL, 'DEPOSIT',       'CARD',  103500, 'SUCCEEDED'),
    (2, 'RC-2026-0002', 1, 1, NULL, NULL, 'RENT',          'CARD', 1035000, 'SUCCEEDED'),
    (3, 'RC-2026-0003', 1, NULL, 1, NULL, 'EXTENSION_FEE', 'CARD',  690000, 'SUCCEEDED'),
    (4, 'RC-2026-0004', 1, 1, NULL, NULL, 'DEPOSIT',       'CARD',   69000, 'SUCCEEDED');

-- ------------------------------------------------------- support ----

INSERT INTO support_tickets (TicketID, Code, CustomerID, UnitID, ReservationID, IncidentType, Status, AssignedStaffID) VALUES
    (1, 'SR-0032', 1, 1, 1,    'DEVICE_ISSUE', 'RESOLVED',  2),  -- sticky door, fixed mid-term
    (2, 'SR-0033', 1, 2, NULL, 'OTHER',        'ESCALATED', 2);  -- flooding in M-2, awaiting manager decision

INSERT INTO escalations (EscalationID, TicketID, EscalatedByStaffID, ManagerID, Note, Decision) VALUES
    (1, 2, 2, 3, 'Water ingress from the ceiling joint of unit M-2. Cannot resolve at desk level - needs a maintenance and relocation decision.', 'PENDING');

-- ------------------------------------------------- task board -------
-- RT-0871 is the checkout task code of the standard mock set (tasks.RefCode).

INSERT INTO tasks (TaskID, Type, RefCode, AssignedStaffID, WorkDate, Status) VALUES
    (1, 'CHECK_IN',  'BK-1042',   2, '2026-10-03', 'DONE'),
    (2, 'CONTRACT',  'CT-1042-A1',2, '2026-10-16', 'DONE'),
    (3, 'CHECKOUT',  'RT-0871',   2, '2026-10-18', 'DONE'),
    (4, 'CLEANING',  'S-3',       2, '2026-10-19', 'TODO'),
    (5, 'SUPPORT',   'SR-0032',   2, '2026-10-06', 'DONE'),
    (6, 'SUPPORT',   'SR-0033',   2, '2026-10-18', 'IN_PROGRESS');

-- -------------------------------------------------- notifications ---
-- DeepLinks are relative FE paths and match contracts/routes.yaml.

INSERT INTO notifications (NotificationID, UserID, Type, Title, DeepLink, IsRead, CreatedAt) VALUES
    ( 1, 1, 'PAYMENT_SUCCEEDED',     'Deposit received - 103.500 VND for unit S-3',             '/rentals/1',    1, '2026-10-01 02:12:30'),
    ( 2, 1, 'CONTRACT_DRAFTED',      'Contract CT-1042 drafted from Rental Policy v3',           '/rentals/1',    1, '2026-10-01 02:12:35'),
    ( 3, 1, 'RESERVATION_CONFIRMED', 'Unit S-3 is reserved - check-in from Oct 3',               '/rentals/1',    1, '2026-10-01 02:12:40'),
    ( 4, 1, 'ACCESS_CODE_ISSUED',    'Access code for unit S-3 - show the check-in pass at the desk', '/rentals/1', 1, '2026-10-03 01:50:00'),
    ( 5, 1, 'TICKET_RESOLVED',       'Support SR-0032 resolved - door hinge adjusted',           '/support/1',    1, '2026-10-07 05:30:00'),
    ( 6, 1, 'EXTENSION_CONFIRMED',   'Extension paid - new checkout date Oct 18',                '/rentals/1',    1, '2026-10-15 09:20:30'),
    ( 7, 1, 'ADDENDUM_SIGNED',       'Addendum CT-1042-A1 signed and filed',                     '/rentals/1',    1, '2026-10-16 04:30:00'),
    ( 8, 1, 'SETTLEMENT_COMPLETED',  'Refund 132.500 VND after damage fee 40.000 VND',           '/rentals/1',    0, '2026-10-18 08:05:30'),
    ( 9, 2, 'TASK_ASSIGNED',         'Checkout RT-0871 assigned - unit S-3, Oct 18',             '/tasks/3',      1, '2026-10-17 03:00:00'),
    (10, 3, 'ESCALATION_OPENED',     'SR-0033 escalated by Minh - flooding in unit M-2',         '/escalations/1',0, '2026-10-19 04:10:00');

-- ------------------------------------------------- activity log -----
-- Append-only audit trail of the same story (Action/EntityType registry is
-- formalized in story 1.2 - these values follow that UPPER_SNAKE convention).

INSERT INTO activity_logs (ActorID, EntityType, EntityID, Action, FromValue, ToValue, Reason) VALUES
    (5, 'USER',               2, 'CREATE',        NULL,     'Staff',            'Initial staff provisioning for the Tan Binh Depot demo'),
    (5, 'USER',               3, 'CREATE',        NULL,     'Facility Manager', 'Initial staff provisioning for the Tan Binh Depot demo'),
    (5, 'USER',               4, 'CREATE',        NULL,     'Business Ops',     'Initial staff provisioning for the Tan Binh Depot demo'),
    (4, 'POLICY',             3, 'CREATE',        NULL,     'v3',               'Published new pricing version effective 2026-10-01'),
    (1, 'RESERVATION',        1, 'CREATE',        NULL,     'RESERVED',         'Booking BK-1042 - deposit 103.500 paid'),
    (1, 'UNIT',               1, 'STATUS_CHANGE', 'AVAILABLE', 'RESERVED',     'Held for booking BK-1042'),
    (1, 'USER',               1, 'LOGIN',         NULL,     NULL,               'Sign-in from web'),
    (2, 'RESERVATION',        1, 'STATUS_CHANGE', 'RESERVED', 'CHECKED_IN',    'Check-in BK-1042 - rent paid in full, contract CT-1042 signed'),
    (2, 'UNIT',               1, 'STATUS_CHANGE', 'RESERVED', 'RENTED',        'Check-in complete for BK-1042'),
    (2, 'CONTRACT',           1, 'STATUS_CHANGE', 'DRAFT',  'SIGNED',           'Signed copy captured at the desk'),
    (1, 'SUPPORT_TICKET',     1, 'STATUS_CHANGE', 'OPEN',   'RESOLVED',         'Door hinge adjusted during morning shift'),
    (1, 'EXTENSION',          1, 'CREATE',        NULL,     'APPLIED',          'Extension to 2026-10-18 - fee 690.000 + deposit top-up 69.000'),
    (2, 'CONTRACT_ADDENDUM',  1, 'STATUS_CHANGE', 'AWAITING_SIGNATURE', 'SIGNED', 'Signed at the desk on customer visit'),
    (1, 'RESERVATION',        1, 'STATUS_CHANGE', 'CHECKED_IN', 'CHECKOUT_REQUESTED', 'Checkout requested for Oct 18'),
    (2, 'RESERVATION',        1, 'STATUS_CHANGE', 'CHECKOUT_REQUESTED', 'CLOSED', 'Settlement RC-2026-0005 - refund 132.500 after damage fee 40.000'),
    (2, 'UNIT',               1, 'STATUS_CHANGE', 'RENTED', 'PREPARING',        'Checkout complete - cleaning pending'),
    (3, 'UNIT',               2, 'STATUS_CHANGE', 'AVAILABLE', 'MAINTENANCE',   'Flood damage assessment after ticket SR-0033'),
    (2, 'SUPPORT_TICKET',     2, 'STATUS_CHANGE', 'OPEN',   'ESCALATED',        'Flooding beyond desk-level fix - escalated to facility manager'),
    (2, 'USER',               2, 'LOGIN',         NULL,     NULL,               'Sign-in from web'),
    (3, 'USER',               3, 'LOGIN',         NULL,     NULL,               'Sign-in from web'),
    (4, 'USER',               4, 'LOGIN',         NULL,     NULL,               'Sign-in from web'),
    (5, 'USER',               5, 'LOGIN',         NULL,     NULL,               'Sign-in from web'),
    (1, 'USER',               1, 'LOGIN_FAILED',  NULL,     NULL,               'One wrong-password attempt before success');
