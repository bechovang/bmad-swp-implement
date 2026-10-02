package com.storagehub.entity;

/**
 * Registry of entity types that can appear in the audit trail (AD-6) -
 * transported UPPER_SNAKE (ActivityLog.EntityType column, VARCHAR(50)). The
 * list is closed: a new audited entity extends this enum, never a free-form
 * string.
 */
public enum EntityType {
    USER,
    UNIT,
    RESERVATION,
    CONTRACT,
    ADDENDUM,
    PAYMENT,
    SETTLEMENT,
    TASK,
    TICKET,
    ESCALATION,
    POLICY,
    INSPECTION
}
