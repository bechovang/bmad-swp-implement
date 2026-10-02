package com.storagehub.entity;

/**
 * rental_policies.Status TINYINT (0=draft, 1=active, 2=retired).
 */
public enum PolicyStatus {
    DRAFT(0),
    ACTIVE(1),
    RETIRED(2);

    private final int dbValue;

    PolicyStatus(int dbValue) {
        this.dbValue = dbValue;
    }

    public int dbValue() {
        return dbValue;
    }

    public static PolicyStatus fromDbValue(int dbValue) {
        for (PolicyStatus status : values()) {
            if (status.dbValue == dbValue) {
                return status;
            }
        }
        throw new IllegalArgumentException("rental_policies.Status " + dbValue + " is outside the 0=draft/1=active/2=retired domain");
    }
}
