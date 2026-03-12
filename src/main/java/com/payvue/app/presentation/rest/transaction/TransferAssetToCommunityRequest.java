package com.payvue.app.presentation.rest.transaction;

import jakarta.validation.constraints.NotBlank;

record TransferAssetToCommunityRequest(
    @NotBlank String assetId,
    @NotBlank String groupId,
    String message,
    String clientRequestId
) {}
