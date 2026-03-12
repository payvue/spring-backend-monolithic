package com.payvue.app.presentation.rest.community;

import jakarta.validation.constraints.NotBlank;

public record PaymentRequestRejectRequest(
    @NotBlank String rejectionReason
) {}
