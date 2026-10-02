package com.storagehub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * roles row (V1__init_schema.sql:22). The Name column stores the Title Case
 * spelling seeded by V2 ("Customer", "Facility Manager", ...); the fixed
 * five-role permission matrix works in {@link RoleName} UPPER_SNAKE and
 * resolves through {@link RoleName#fromTitleCase(String)}. Kept a plain
 * String on this side so JPA can read any roles row; membership of the
 * matrix is decided by the enum, not the loader.
 */
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @Column(name = "RoleID", nullable = false)
    private Integer id;

    @Column(name = "Name", nullable = false, length = 50)
    private String name;

    @Column(name = "Description", length = 200)
    private String description;

    /** JPA only. */
    protected Role() {
    }

    public Role(Integer id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    /** Matrix role of this row ({@link RoleName#fromTitleCase(String)}). */
    public RoleName roleName() {
        return RoleName.fromTitleCase(name);
    }

    public Integer getId() {
        return id;
    }

    /** Title Case value of the V2 seed ("Facility Manager"). */
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
