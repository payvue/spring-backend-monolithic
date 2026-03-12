package com.payvue.app.presentation.rest.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentRequestCreateRequest(
    @NotNull @Positive Long amount,
    @NotBlank String description,
    @NotBlank String category,
    @NotBlank String urgency
) {}
