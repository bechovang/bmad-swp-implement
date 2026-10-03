package com.storagehub.dto;

import com.storagehub.entity.InspectionItem;
import com.storagehub.entity.InspectionResult;
import java.time.LocalDateTime;

public record InspectionItemDto(
        Long id,
        InspectionItem item,
        InspectionResult result,
        String note,
        Long inspectorStaffId,
        String inspectorStaffName,
        LocalDateTime createdAt
) {
}
