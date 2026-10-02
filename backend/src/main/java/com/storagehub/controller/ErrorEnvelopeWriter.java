package com.storagehub.controller;

import com.storagehub.dto.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Writes the error envelope directly to the servlet response. Needed by the
 * security handlers (AuthenticationEntryPoint / AccessDeniedHandler), which
 * run outside the MVC exception-resolver flow and cannot return a
 * ResponseEntity. Shares the ApiError record with GlobalExceptionHandler so
 * security errors and MVC errors render byte-for-byte the same envelope.
 */
@Component
public class ErrorEnvelopeWriter {

    private final ObjectMapper objectMapper;

    public ErrorEnvelopeWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, HttpStatusCode status, ApiError error) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), error);
    }
}
