-- Story 5.3: Escalations manager decision and relocation details
ALTER TABLE escalations
    ADD COLUMN ManagerNote TEXT NULL COMMENT 'Facility Manager decision rationale / staff instructions',
    ADD COLUMN RelocatedToUnitID BIGINT NULL COMMENT 'Target unit ID if customer relocated due to severe maintenance',
    ADD COLUMN CreatedAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN ResolvedAt DATETIME NULL;

ALTER TABLE escalations
    ADD CONSTRAINT fk_escalations_relocated_unit FOREIGN KEY (RelocatedToUnitID) REFERENCES units (UnitID);
