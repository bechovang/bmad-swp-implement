package com.storagehub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * activity_logs row (V1__init_schema.sql:332). Append-only by construction:
 * every column is insert-only at the JPA level (updatable = false) and the
 * repository exposes nothing but save - LogService is the sole writer (AD-6,
 * NFR-6). The table intentionally has no timestamp column (deferred [D5]);
 * LogID ordering is the chronology. actorId stays a plain Long (no JPA
 * association) - login audits write it directly, including NULL for
 * LOGIN_FAILED rows with an unknown email (V3, Q1=A).
 */
@Entity
@Table(name = "activity_logs")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LogID", nullable = false, updatable = false)
    private Long id;

    /**
     * Nullable since V3 (Q1=A): LOGIN_FAILED rows for unknown emails have no
     * actor - the Reason carries the attempted email. Every other action
     * keeps a real user id (FK still enforced when non-null).
     */
    @Column(name = "ActorID", updatable = false)
    private Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "EntityType", nullable = false, length = 50, updatable = false)
    private EntityType entityType;

    @Column(name = "EntityID", nullable = false, updatable = false)
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "Action", nullable = false, length = 50, updatable = false)
    private Action action;

    @Column(name = "FromValue", length = 255, updatable = false)
    private String fromValue;

    @Column(name = "ToValue", length = 255, updatable = false)
    private String toValue;

    /** NOT NULL in DDL: LogService stores "" when the action needs no reason. */
    @Column(name = "Reason", nullable = false, length = 255, updatable = false)
    private String reason;

    /** JPA only. Use the all-args constructor (called by LogService). */
    protected ActivityLog() {
    }

    public ActivityLog(Long actorId, EntityType entityType, Long entityId, Action action,
            String fromValue, String toValue, String reason) {
        this.actorId = actorId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.fromValue = fromValue;
        this.toValue = toValue;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public Long getActorId() {
        return actorId;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public Action getAction() {
        return action;
    }

    public String getFromValue() {
        return fromValue;
    }

    public String getToValue() {
        return toValue;
    }

    public String getReason() {
        return reason;
    }
}
