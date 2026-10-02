package com.storagehub.dto;

import java.util.List;

/**
 * List envelope for every list endpoint (AD-8) - exactly the four fields of
 * the ListEnvelope schema in contracts/openapi.yaml: items of the current
 * 1-based page, the page/pageSize in effect and the total matching rows.
 * Per-resource usages refine {@code items}, never these four fields.
 */
public record PageResponse<T>(List<T> items, int page, int pageSize, long total) {

    public static <T> PageResponse<T> of(List<T> items, ListQuery query, long total) {
        return new PageResponse<>(List.copyOf(items), query.page(), query.pageSize(), total);
    }
}
