package com.storagehub.dto;

import com.storagehub.entity.InspectionItem;
import java.time.LocalDate;
import java.util.List;

public record CheckoutTaskDetailDto(
        Long taskId,
        String taskStatus,
        Long reservationId,
        String reservationCode,
        Long customerId,
        String customerName,
        String customerPhone,
        Long unitId,
        String unitCode,
        LocalDate requestedDate,
        Boolean keyReturned,
        Boolean unitEmptied,
        List<InspectionItemDto> inspections,
        Boolean hasMajorDamage,
        List<InspectionItem> majorItems
) {
}
