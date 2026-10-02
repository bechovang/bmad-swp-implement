package com.storagehub.dto;

import java.time.LocalDate;

public record ExtensionBoundaryDto(
        Long rentalId,
        String unitCode,
        LocalDate currentEndDate,
        LocalDate latestPossibleCheckoutDate,
        LocalDate conflictStartDate,
        String conflictReservationCode,
        boolean isExtendable,
        String message
) {}
