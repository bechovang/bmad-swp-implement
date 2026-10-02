package com.storagehub.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Jackson (v3, the Boot 4 default) rendering of the envelopes against the
 * frozen schemas in contracts/openapi.yaml: PageResponse serializes to
 * exactly the four camelCase ListEnvelope fields, ApiError to code/message
 * with fieldErrors appearing only when validation actually failed. Runs in
 * the JSON slice - no database needed.
 */
@JsonTest
class EnvelopeSerializationTests {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void pageResponseSerializesToExactlyTheFourListEnvelopeFields() {
        String json = objectMapper.writeValueAsString(
                PageResponse.of(List.of("S-3", "M-2", "M-5"), new ListQuery(1, 25), 3));

        JsonNode node = objectMapper.readTree(json);
        assertThat(fieldNames(node)).containsExactly("items", "page", "pageSize", "total");
        assertThat(node.get("items").isArray()).isTrue();
        assertThat(node.get("items").size()).isEqualTo(3);
        assertThat(node.get("page").asInt()).isEqualTo(1);
        assertThat(node.get("pageSize").asInt()).isEqualTo(25);
        assertThat(node.get("total").asLong()).isEqualTo(3L);
    }

    @Test
    void apiErrorWithoutFieldErrorsOmitsThemFromTheJson() {
        String json = objectMapper.writeValueAsString(ApiError.of(ApiErrorCode.NOT_FOUND));

        assertThat(json).doesNotContain("fieldErrors");
        JsonNode node = objectMapper.readTree(json);
        assertThat(fieldNames(node)).containsExactly("code", "message");
        assertThat(node.get("code").asString()).isEqualTo("NOT_FOUND");
    }

    @Test
    void apiErrorWithFieldErrorsRendersThemAsFieldMessagePairs() {
        ApiError error = ApiError.of(ApiErrorCode.VALIDATION_FAILED, List.of(
                new FieldError("email", "email must be a valid address"),
                new FieldError("count", "count must be at least 1")));

        JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(error));
        assertThat(fieldNames(node)).containsExactly("code", "fieldErrors", "message");
        assertThat(node.get("fieldErrors").size()).isEqualTo(2);
        assertThat(node.get("fieldErrors").get(0).get("field").asString()).isEqualTo("email");
        assertThat(node.get("fieldErrors").get(0).get("message").asString())
                .isEqualTo("email must be a valid address");
        assertThat(fieldNames(node.get("fieldErrors").get(0))).containsExactly("field", "message");
    }

    private static SortedSet<String> fieldNames(JsonNode node) {
        SortedSet<String> names = new TreeSet<>();
        node.properties().forEach(entry -> names.add(entry.getKey()));
        return names;
    }
}
