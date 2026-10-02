package com.storagehub.service.payment;

import vn.payos.model.v2.paymentRequests.PaymentLink;

public interface PaymentGateway {

    PaymentLinkResult createPaymentLink(long orderCode, int amount, String description,
                                        String returnUrl, String cancelUrl);

    PaymentLink getPaymentLinkInformation(long orderCode);

    void cancelPaymentLink(long orderCode, String cancellationReason);
}
