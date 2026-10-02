package com.storagehub.controller;

import com.storagehub.security.EnvelopeAccessDeniedHandler;
import com.storagehub.security.EnvelopeAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The 403 row of the story 1.2 error matrix: an authenticated caller without
 * the required role hits EnvelopeAccessDeniedHandler and receives the FORBIDDEN
 * envelope, not Spring Security's default body. Uses TestSecurityConfig (the
 * only place a role rule exists until the story 1.3 permission matrix).
 */
@WebMvcTest(controllers = TestPingController.class)
@Import({ TestSecurityConfig.class, EnvelopeAuthenticationEntryPoint.class,
        EnvelopeAccessDeniedHandler.class, ErrorEnvelopeWriter.class })
@WithMockUser(roles = "CUSTOMER")
class AccessDeniedEnvelopeTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authenticatedButWrongRoleYields403Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/__test/role-guarded"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }
}
