package com.storagehub.security;

import com.storagehub.controller.ErrorEnvelopeWriter;
import com.storagehub.dto.ApiError;
import com.storagehub.dto.ApiErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 401 for unauthenticated requests on guarded paths, written as the standard
 * error envelope instead of Spring Security's default body (AD-8). Sends the
 * RFC 7235 challenge header so clients know the API wants a Bearer token;
 * the token itself arrives in story 1.3.
 */
@Component
public class EnvelopeAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ErrorEnvelopeWriter errorEnvelopeWriter;

    public EnvelopeAuthenticationEntryPoint(ErrorEnvelopeWriter errorEnvelopeWriter) {
        this.errorEnvelopeWriter = errorEnvelopeWriter;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        errorEnvelopeWriter.write(response, HttpStatus.UNAUTHORIZED, ApiError.of(ApiErrorCode.UNAUTHENTICATED));
    }
}
