package com.storagehub.entity;

/**
 * Registry of audited actions (AD-6). The registry - not the caller - decides
 * which actions carry a mandatory reason: append() refuses to insert a log row
 * when a reason-required action arrives without a non-blank one, so no orphan
 * FIX_STATUS/WAIVER entry can exist. Values are transported UPPER_SNAKE
 * (ActivityLog.Action column, VARCHAR(50)).
 */
public enum Action {

    /** Successful sign-in (FR-38, story 1.3 audits through LogService). */
    LOGIN(false),

    /** Failed sign-in attempt - one generic row, never which field failed (AD-5). */
    LOGIN_FAILED(false),

    /** Regular state-machine transition of the owning entity. */
    STATUS_CHANGE(false),

    /** Customer signed a contract or addendum draft. */
    CONTRACT_SIGNED(false),

    /** Customer moved to another unit within a rental. */
    RELOCATION(false),

    /** Manual unit status correction - reason mandatory (which defect, which inspection). */
    FIX_STATUS(true),

    /** Fee waived - reason mandatory (who approved, on which basis). */
    WAIVER(true),

    /** Damage charged at checkout settlement - reason mandatory (what was damaged). */
    DAMAGE_CHARGE(true),

    /** Ticket escalated - reason mandatory (why normal support was not enough). */
    ESCALATION(true),

    /** Addendum voided before signature - reason mandatory (why the draft died). */
    ADDENDUM_VOID(true),

    /** Unit checkout inspection completed. */
    INSPECTION_COMPLETED(false);

    private final boolean requiresReason;

    Action(boolean requiresReason) {
        this.requiresReason = requiresReason;
    }

    public boolean requiresReason() {
        return requiresReason;
    }
}
