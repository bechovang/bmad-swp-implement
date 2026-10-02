package com.storagehub.dto;

import java.util.List;

public record BrowseUnitsResponse(
        List<BrowseUnitDto> items,
        int totalAvailable,
        int totalUnits
) {
}
