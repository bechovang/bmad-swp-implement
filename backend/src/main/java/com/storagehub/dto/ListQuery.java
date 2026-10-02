package com.storagehub.dto;

/**
 * Offset pagination input for every list endpoint (AD-8): {@code page} is
 * 1-based and defaults to {@link #DEFAULT_PAGE}, {@code pageSize} defaults to
 * {@link #DEFAULT_PAGE_SIZE} (25 rows, FR-26/28/31). Bound violations fail
 * fast in the constructor; controllers that want a 400 envelope instead bind
 * the params with {@code @Min(1)} before calling {@link #of(Integer, Integer)}.
 */
public record ListQuery(int page, int pageSize) {

    public static final int DEFAULT_PAGE = 1;

    public static final int DEFAULT_PAGE_SIZE = 25;

    public ListQuery {
        if (page < 1) {
            throw new IllegalArgumentException("page must be >= 1, got: " + page);
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be >= 1, got: " + pageSize);
        }
    }

    public static ListQuery of(Integer page, Integer pageSize) {
        return new ListQuery(
                page == null ? DEFAULT_PAGE : page,
                pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
    }

    /**
     * Raw query-string variant used by the argument resolver: null/blank means
     * "not sent" (default applies), anything non-numeric or out of bounds is
     * an IllegalArgumentException so the caller can map it to 400.
     */
    public static ListQuery of(String page, String pageSize) {
        return of(parseInt("page", page), parseInt("pageSize", pageSize));
    }

    private static Integer parseInt(String name, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(raw.trim());
        }
        catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    name + " must be an integer >= 1, got: '" + raw.trim() + "'", ex);
        }
    }
}
