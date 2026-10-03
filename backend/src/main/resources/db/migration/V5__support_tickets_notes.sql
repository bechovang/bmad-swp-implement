-- Story 5.2: Support Ticket resolution note column
ALTER TABLE support_tickets
    ADD COLUMN ResolutionNote TEXT NULL COMMENT 'Staff note recorded upon resolving the incident';
