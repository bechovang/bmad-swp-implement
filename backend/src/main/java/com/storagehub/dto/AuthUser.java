package com.storagehub.dto;

import com.storagehub.entity.User;

/**
 * The user half of the auth responses (contract: AuthUser). role travels
 * UPPER_SNAKE (RoleName name()) - the same convention as the JWT role claim
 * and contracts/routes.yaml, so FE 1.5 keys its role shell on one string.
 */
public record AuthUser(Long id, String fullName, String email, String role, String phone) {

    public static AuthUser from(User user) {
        return new AuthUser(user.getId(), user.getFullName(), user.getEmail(),
                user.roleName().name(), user.getPhone());
    }
}
