package com.payvue.app.presentation.rest.community;

import com.payvue.app.domain.paymentrequest.PaymentRequest;

import java.time.Instant;

public record PaymentRequestResponse(
    String id,
    String groupId,
    String requesterId,
    String requesterName,
    Long amount,
    String description,
    String category,
    String urgency,
    String status,
    Instant requestDate,
    Instant approvedDate,
    Instant rejectedDate,
    String rejectionReason
) {

    public static PaymentRequestResponse from(PaymentRequest request, String requesterName) {
        return new PaymentRequestResponse(
                request.id().toString(),
                request.groupId(),
                request.requesterId(),
                requesterName,
                request.amount(),
                request.description(),
                request.category(),
                request.urgency(),
                request.status().name(),
                request.requestDate(),
                request.approvedDate(),
                request.rejectedDate(),
                request.rejectionReason()
        );
    }

}
