package com.payvue.app.presentation.rest.hold;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssetOnlineCreateRequest(
    @NotBlank String name,
    @NotBlank String category,
    @NotNull @Positive Long amount,
    String description,
    String status
) {}
