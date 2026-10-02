package com.storagehub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * users row (V1__init_schema.sql:49). Column names are declared verbatim
 * (PascalCase, rule of story 1.2 - PhysicalNamingStrategyStandardImpl).
 * The Role association is LAZY: the filter/status checks read identity
 * columns and never need the roles row unless the matrix role is asked for.
 * FacilityID stays a plain nullable Integer ([D4]) until a Facility entity
 * exists in its own story. JPA entities never leave the backend (AD-3) -
 * controllers return DTOs.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserID", nullable = false, updatable = false)
    private Long id;

    @Column(name = "FullName", nullable = false, length = 100)
    private String fullName;

    @Column(name = "Email", nullable = false, length = 100)
    private String email;

    @Column(name = "Phone", length = 20)
    private String phone;

    /** BCrypt hash ($2a$10$ seeded; register hashes with strength 10). */
    @Column(name = "PasswordHash", nullable = false, length = 255)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RoleID", nullable = false, updatable = false)
    private Role role;

    @Convert(converter = UserStatusConverter.class)
    @Column(name = "Status", nullable = false)
    private UserStatus status;

    /** DB default CURRENT_TIMESTAMP fills it (insertable = false); UTC by session pin. */
    @Column(name = "CreatedAt", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    /** Nullable facility scope [D4]; demo runs one facility, multi-facility FM ready. */
    @Column(name = "FacilityID")
    private Integer facilityId;

    /** JPA only. */
    protected User() {
    }

    public User(String fullName, String email, String phone, String passwordHash, Role role,
            UserStatus status) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
    }

    /** Matrix role of this user (Title Case row resolved to UPPER_SNAKE). */
    public RoleName roleName() {
        return role.roleName();
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Integer getFacilityId() {
        return facilityId;
    }
}
