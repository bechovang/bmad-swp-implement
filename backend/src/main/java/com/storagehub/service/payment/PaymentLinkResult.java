package com.storagehub.service.payment;

public record PaymentLinkResult(
        String checkoutUrl,
        String qrCode,
        long orderCode,
        int amount,
        String status
) {
}
