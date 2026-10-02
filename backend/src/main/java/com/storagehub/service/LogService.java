package com.storagehub.service;

import com.storagehub.entity.Action;
import com.storagehub.entity.ActivityLog;
import com.storagehub.entity.EntityType;
import com.storagehub.repository.ActivityLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sole writer of the audit trail (AD-6). Appends synchronously inside the
 * caller's transaction - plain REQUIRED propagation, never REQUIRES_NEW and
 * never an after-commit event - so a business change and its audit row commit
 * or roll back together. Reason policy lives in the {@link Action} registry:
 * a reason-required action with a blank reason is refused here, before the
 * repository is ever touched, so no orphan log row can be inserted. Actions
 * that do not need a reason still satisfy the NOT NULL column: "" is stored.
 */
@Service
public class LogService {

    /** V1 DDL size of the Reason/FromValue/ToValue VARCHAR(255) columns. */
    static final int MAX_TEXT_LENGTH = 255;

    private final ActivityLogRepository activityLogRepository;

    public LogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    /**
     * @param actorId nullable since V3 (Q1=A): LOGIN_FAILED with an unknown
     *        email has no actor - Reason then carries the attempted email.
     *        Every other caller passes a real user id.
     * @throws IllegalArgumentException the action requires a reason and
     *         {@code reason} is null or blank, or any of fromValue/toValue/reason
     *         exceeds {@value #MAX_TEXT_LENGTH} characters (the DDL column size)
     *         - refused, nothing is inserted
     */
    @Transactional
    public void append(Long actorId, EntityType entityType, Long entityId, Action action,
            String fromValue, String toValue, String reason) {
        if (action.requiresReason() && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException(
                    "Action " + action.name() + " requires a non-blank reason (Action registry); refusing to log.");
        }
        if (lengthOf(fromValue) > MAX_TEXT_LENGTH || lengthOf(toValue) > MAX_TEXT_LENGTH
                || lengthOf(reason) > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException(
                    "fromValue, toValue and reason must each fit the VARCHAR(" + MAX_TEXT_LENGTH
                            + ") audit columns; refusing to log.");
        }
        activityLogRepository.save(new ActivityLog(actorId, entityType, entityId, action,
                fromValue, toValue, reason == null ? "" : reason));
    }

    private static int lengthOf(String value) {
        return value == null ? 0 : value.length();
    }
}
