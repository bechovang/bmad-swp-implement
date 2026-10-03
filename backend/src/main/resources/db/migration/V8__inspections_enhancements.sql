-- V8: Enhancements for inspections and checkout task reception
ALTER TABLE inspections MODIFY COLUMN SettlementID BIGINT NULL;

ALTER TABLE inspections
    ADD COLUMN ReservationID BIGINT NOT NULL AFTER InspectionID,
    ADD COLUMN InspectorStaffID BIGINT NULL AFTER Note,
    ADD COLUMN CreatedAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER InspectorStaffID,
    ADD CONSTRAINT fk_inspections_reservation FOREIGN KEY (ReservationID) REFERENCES reservations (ReservationID),
    ADD CONSTRAINT fk_inspections_inspector FOREIGN KEY (InspectorStaffID) REFERENCES users (UserID);

ALTER TABLE checkout_requests
    ADD COLUMN KeyReturned BOOLEAN NOT NULL DEFAULT FALSE AFTER Status,
    ADD COLUMN UnitEmptied BOOLEAN NOT NULL DEFAULT FALSE AFTER KeyReturned;
