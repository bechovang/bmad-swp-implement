-- =====================================================================
-- StorageHub V4 (Story 5.1): Add Description, CreatedAt, UpdatedAt to support_tickets
--
-- Why: Customers submit an issue description when creating a support ticket.
-- Tracking creation and update timestamps provides accurate timeline views.
-- =====================================================================

ALTER TABLE support_tickets
    ADD COLUMN Description TEXT NULL,
    ADD COLUMN CreatedAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN UpdatedAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
