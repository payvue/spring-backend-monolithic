package com.payvue.app.presentation.rest.transaction;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

record TransferAssetToUserRequest(
    @NotBlank String assetId,
    @Email @NotBlank String recipientEmail,
    String message,
    String clientRequestId
) {}
