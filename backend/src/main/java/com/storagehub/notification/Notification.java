package com.storagehub.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * notifications row (V1__init_schema.sql:319).
 * UserID, Type, Title, DeepLink and CreatedAt are immutable once written;
 * only IsRead can be updated by the owning user.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NotificationID", nullable = false, updatable = false)
    private Long id;

    @Column(name = "UserID", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "Type", nullable = false, length = 50, updatable = false)
    private String type;

    @Column(name = "Title", nullable = false, length = 200, updatable = false)
    private String title;

    @Column(name = "DeepLink", length = 255, updatable = false)
    private String deepLink;

    @Column(name = "IsRead", nullable = false)
    private boolean isRead = false;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA only */
    protected Notification() {
    }

    public Notification(Long userId, String type, String title, String deepLink) {
        this(null, userId, type, title, deepLink, false, Instant.now());
    }

    public Notification(Long id, Long userId, String type, String title, String deepLink, boolean isRead, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.deepLink = deepLink;
        this.isRead = isRead;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDeepLink() {
        return deepLink;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        this.isRead = read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
