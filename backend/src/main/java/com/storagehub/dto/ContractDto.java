package com.storagehub.dto;

import com.storagehub.entity.ContractStatus;

public record ContractDto(
        Long id,
        String code,
        Long reservationId,
        String reservationCode,
        Integer policyId,
        String policyVersion,
        String contentSnapshot,
        ContractContentSnapshotDto snapshot,
        String signedPhotoUrl,
        ContractStatus status,
        Long supersedesContractId,
        Integer isLatest
) {
}
