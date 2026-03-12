package com.payvue.app.presentation.rest.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

record ChangePasswordRequest(
    @Schema(description = "현재 비밀번호", example = "OldPassw0rd!")
    @JsonProperty("current_password") String currentPassword,
    @Schema(description = "새 비밀번호", example = "NewPassw0rd!")
    @JsonProperty("new_password") String newPassword
) {}
