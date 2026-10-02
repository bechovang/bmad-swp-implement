package com.storagehub.controller;

import com.storagehub.config.SecurityConfig;
import com.storagehub.security.EnvelopeAccessDeniedHandler;
import com.storagehub.security.EnvelopeAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc coverage of the story 1.2 error matrix against the REAL
 * SecurityConfig chain (401 row included): every non-2xx leaves as the one
 * Error envelope declared in contracts/openapi.yaml - no default Spring body.
 * Runs in the web slice, no database needed. The 403 row needs a role rule,
 * which only exists in TestSecurityConfig - see AccessDeniedEnvelopeTests.
 */
@WebMvcTest(controllers = TestPingController.class)
@Import({ SecurityConfig.class, EnvelopeAuthenticationEntryPoint.class,
        EnvelopeAccessDeniedHandler.class, ErrorEnvelopeWriter.class })
@WithMockUser
class GlobalExceptionHandlerTests {

    @Autowired
    private MockMvc mockMvc;

    // ------------------------------------------------------------- 401 row

    @Test
    @WithAnonymousUser
    void unauthenticatedRequestYields401EnvelopeAndChallengeHeader() throws Exception {
        mockMvc.perform(get("/api/v1/__test/ping"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    // ------------------------------------------------------------- 400 rows

    @Test
    void beanValidationFailureYields400WithEveryFieldError() throws Exception {
        mockMvc.perform(post("/api/v1/__test/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"email\":\"not-an-email\",\"count\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(3))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'count')]").exists());
    }

    @Test
    void unreadableJsonYields400MalformedRequest() throws Exception {
        mockMvc.perform(post("/api/v1/__test/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void missingRequiredParameterYields400MalformedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/__test/requires-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void wrongParameterTypeYields400MalformedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/__test/requires-param").param("seq", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void constrainedHandlerParameterYields400ValidationFailedWithFieldError() throws Exception {
        mockMvc.perform(get("/api/v1/__test/validated-param").param("seq", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'seq')]").exists())
                .andExpect(jsonPath("$.fieldErrors[0].message").value("seq must be at least 1"));
    }

    // -------------------------------------------------- other HTTP rows

    @Test
    void unknownPathYields404Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/__does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void wrongMethodYields405Envelope() throws Exception {
        mockMvc.perform(delete("/api/v1/__test/ping"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void wrongMediaTypeYields415Envelope() throws Exception {
        mockMvc.perform(post("/api/v1/__test/validated")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not json"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void businessRuleBlockYields409WithCallerCode() throws Exception {
        mockMvc.perform(get("/api/v1/__test/business-block"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNIT_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value(
                        "The unit is no longer available for this period. Nothing was reserved. "
                                + "Pick another unit or another date."))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void unexpectedFailureYields500WithoutInternalDetail() throws Exception {
        mockMvc.perform(get("/api/v1/__test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value(
                        "An unexpected error occurred. Nothing was changed. "
                                + "Retry in a moment or contact support if it keeps failing."))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist())
                .andExpect(content().string(not(containsString("secret internal detail"))));
    }

    // ------------------------------------------------------------ list rows

    @Test
    void listDefaultsToPageOnePageSizeTwentyFive() throws Exception {
        mockMvc.perform(get("/api/v1/__test/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(25))
                .andExpect(jsonPath("$.total").value(3));
    }

    @Test
    void listEchoesExplicitPageAndPageSize() throws Exception {
        mockMvc.perform(get("/api/v1/__test/list")
                        .param("page", "2")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.pageSize").value(2))
                .andExpect(jsonPath("$.total").value(3));
    }

    @Test
    void listWithPageBelowOneYields400MalformedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/__test/list").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void listWithNonNumericPageYields400MalformedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/__test/list").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }
}
