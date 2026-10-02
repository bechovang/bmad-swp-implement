package com.storagehub.service.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;

@Service
public class PayOsPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(PayOsPaymentGateway.class);

    private final PayOS payOS;

    public PayOsPaymentGateway(PayOS payOS) {
        this.payOS = payOS;
    }

    @Override
    public PaymentLinkResult createPaymentLink(long orderCode, int amount, String description,
                                               String returnUrl, String cancelUrl) {
        try {
            CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount((long) amount)
                    .description(description)
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .build();

            CreatePaymentLinkResponse response = payOS.paymentRequests().create(request);
            return new PaymentLinkResult(
                    response.getCheckoutUrl(),
                    response.getQrCode(),
                    response.getOrderCode(),
                    response.getAmount() != null ? response.getAmount().intValue() : amount,
                    response.getStatus() != null ? response.getStatus().name() : "PENDING"
            );
        } catch (Exception e) {
            log.error("Failed to create PayOS payment link for orderCode {}: {}", orderCode, e.getMessage(), e);
            throw new RuntimeException("PayOS payment link creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentLink getPaymentLinkInformation(long orderCode) {
        try {
            return payOS.paymentRequests().get(orderCode);
        } catch (Exception e) {
            log.error("Failed to get PayOS payment link info for orderCode {}: {}", orderCode, e.getMessage(), e);
            throw new RuntimeException("PayOS status query failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void cancelPaymentLink(long orderCode, String cancellationReason) {
        try {
            payOS.paymentRequests().cancel(orderCode, cancellationReason);
        } catch (Exception e) {
            log.error("Failed to cancel PayOS payment link for orderCode {}: {}", orderCode, e.getMessage(), e);
            // Non-critical: log and proceed
        }
    }
}
