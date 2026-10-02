package com.storagehub.repository;

import com.storagehub.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * roles data access (story 1.3). Roles are reference data owned by the V2
 * seed - the repository is read-only in practice; save exists only through
 * JpaRepository inheritance and is unused.
 */
public interface RoleRepository extends JpaRepository<Role, Integer> {

    /** Title Case name exactly as seeded ("Customer", "Facility Manager", ...). */
    Optional<Role> findByName(String name);
}
