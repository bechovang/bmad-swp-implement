-- V10: Settlement waiver support and WAIVER_CAP
ALTER TABLE settlements
    ADD COLUMN WaiverAmount DECIMAL(15, 0) NOT NULL DEFAULT 0 AFTER LateFee,
    ADD COLUMN WaiverReason VARCHAR(255) NULL AFTER WaiverAmount;
