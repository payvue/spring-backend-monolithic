package com.payvue.app.presentation.rest.auth;

import io.swagger.v3.oas.annotations.media.Schema;

record LoginResponse(
    @Schema(description = "Access token", example = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...")
    String access
) {}
