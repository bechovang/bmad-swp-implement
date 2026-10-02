package com.storagehub.dto;

/**
 * POST /api/v1/auth/login 200 body (contract: LoginResponse). Logout is the
 * client dropping the token - there is no refresh token (AD-5).
 */
public record LoginResponse(String token, AuthUser user) {
}
