package com.storagehub.dto;

/**
 * POST /api/v1/auth/register 201 body (contract: RegisterResponse). The
 * created account is always CUSTOMER / active; the FE lands the user on
 * Browse Units logged in (it may call login right after, or we add a token
 * here in a later story if the contract grows one).
 */
public record RegisterResponse(Long id, String fullName, String email, String role, String phone) {

    public static RegisterResponse from(AuthUser user) {
        return new RegisterResponse(user.id(), user.fullName(), user.email(), user.role(), user.phone());
    }
}
