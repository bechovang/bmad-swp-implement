package com.storagehub.dto;

import java.time.LocalDate;

public record ExtensionQuoteDto(
        Long rentalId,
        String unitCode,
        LocalDate currentEndDate,
        LocalDate newEndDate,
        int additionalDays,
        double additionalMonths,
        Long monthlyRate,
        Long additionalRent,
        Long currentHeldDeposit,
        Long newTotalDepositRequired,
        Long depositTopUp,
        Long totalFee,
        String currency,
        String policyVersion
) {}
