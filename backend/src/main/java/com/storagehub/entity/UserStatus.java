package com.storagehub.entity;

/**
 * users.Status TINYINT (V1 DDL comment: 0=inactive, 1=active, 2=locked).
 * Only ACTIVE may sign in and stay authenticated (AD-5): login refuses the
 * other two with the shared generic message, and the per-request filter
 * re-checks this so a lock lands within the status-cache staleness window
 * (~30s), not at token TTL.
 */
public enum UserStatus {

    INACTIVE(0),
    ACTIVE(1),
    LOCKED(2);

    private final int dbValue;

    UserStatus(int dbValue) {
        this.dbValue = dbValue;
    }

    /** TINYINT code stored in users.Status. */
    public int dbValue() {
        return dbValue;
    }

    /**
     * @throws IllegalArgumentException the TINYINT is outside the 0/1/2 domain
     *         of the frozen DDL comment
     */
    public static UserStatus fromDbValue(int dbValue) {
        for (UserStatus status : values()) {
            if (status.dbValue == dbValue) {
                return status;
            }
        }
        throw new IllegalArgumentException(
                "users.Status " + dbValue + " is outside the 0=inactive/1=active/2=locked domain");
    }
}
