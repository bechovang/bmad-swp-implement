package com.storagehub.security;

import com.storagehub.controller.ErrorEnvelopeWriter;
import com.storagehub.dto.ApiError;
import com.storagehub.dto.ApiErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 403 for authenticated-but-not-allowed requests, written as the standard
 * error envelope instead of Spring Security's default body (AD-8). The real
 * role rules are the permission matrix of story 1.3 - this handler is the
 * rendering layer they will trip.
 */
@Component
public class EnvelopeAccessDeniedHandler implements AccessDeniedHandler {

    private final ErrorEnvelopeWriter errorEnvelopeWriter;

    public EnvelopeAccessDeniedHandler(ErrorEnvelopeWriter errorEnvelopeWriter) {
        this.errorEnvelopeWriter = errorEnvelopeWriter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        errorEnvelopeWriter.write(response, HttpStatus.FORBIDDEN, ApiError.of(ApiErrorCode.FORBIDDEN));
    }
}
