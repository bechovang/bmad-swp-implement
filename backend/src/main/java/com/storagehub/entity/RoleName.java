package com.storagehub.entity;

import java.util.Arrays;

/**
 * The five fixed roles of the permission matrix (story 1.3). Each constant
 * carries BOTH spellings the system uses: the Title Case {@code roles.Name}
 * stored by the V2 seed ("Facility Manager") and the UPPER_SNAKE transport
 * form of the JWT {@code role} claim, the login response and
 * contracts/routes.yaml ("FACILITY_MANAGER"). The matrix maps each role to
 * the single Spring Security authority {@code ROLE_<ROLE>} - today that is
 * the whole matrix; story 2.x+ adds fine-grained keys and facility/zone
 * scopes anchored on this same enum.
 */
public enum RoleName {

    CUSTOMER("Customer"),
    STAFF("Staff"),
    FACILITY_MANAGER("Facility Manager"),
    BUSINESS_OPS("Business Ops"),
    SYSTEM_ADMINISTRATOR("System Administrator");

    private final String titleCase;

    RoleName(String titleCase) {
        this.titleCase = titleCase;
    }

    /** Value stored in {@code roles.Name} by the V2 seed (Title Case). */
    public String titleCase() {
        return titleCase;
    }

    /** Spring Security authority granted for this role: {@code ROLE_<ROLE>}. */
    public String authority() {
        return "ROLE_" + name();
    }

    /**
     * Resolves a {@code roles.Name} row value (Title Case, per the V2 seed)
     * into the matrix role.
     *
     * @throws IllegalArgumentException the row is not one of the five seeded
     *         roles - the matrix is closed, an unknown role cannot be granted
     */
    public static RoleName fromTitleCase(String titleCase) {
        return Arrays.stream(values())
                .filter(role -> role.titleCase.equals(titleCase))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown roles.Name '" + titleCase + "' - not part of the fixed five-role matrix"));
    }
}
