package com.payvue.app.presentation.rest.transaction;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payvue.app.domain.community.RecipientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

record TransferCashRequest(
    @NotBlank String recipientId,
    @JsonProperty(defaultValue = "COMMUNITY") RecipientType recipientType,
    @NotNull @Positive Long amount,
    String currency,
    String message,
    String clientRequestId
) {
}
