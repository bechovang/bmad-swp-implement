package com.storagehub.repository;

import com.storagehub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * users data access (story 1.3). Full JpaRepository: users are normal
 * mutable domain rows (unlike the append-only activity_logs). Reads stay
 * exact-match equality - the utf8mb4_0900_ai_ci collation decides
 * case-insensitivity at the DB level, matching uk_users_email.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** Register duplicate-email guard (no role needed). */
    Optional<User> findByEmail(String email);

    /**
     * Login lookup with the roles row JOIN-fetched: login runs outside a
     * transaction (the LOGIN_FAILED audit must commit even when the branch
     * throws) and open-in-view is off, so the LAZY role association must be
     * initialized by the query itself for roleName() to work.
     */
    @Query("select u from User u join fetch u.role where u.email = :email")
    Optional<User> findByEmailWithRole(@Param("email") String email);
}
