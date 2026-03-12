package com.payvue.app.presentation.rest.account;

import io.swagger.v3.oas.annotations.media.Schema;

record AccountExistRequest(
    @Schema(description = "이메일", example = "user@example.com")
    String email
) {}
