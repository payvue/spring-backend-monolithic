package com.payvue.app.presentation.rest.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

record LoginRequest(
    @Schema(description = "사용자 아이디", example = "user@example.com")
    @NotBlank @Email String email,
    @Schema(description = "비밀번호", example = "Passw0rd!")
    @NotBlank @JsonAlias({"Passw0rd!"}) String password
) {}
