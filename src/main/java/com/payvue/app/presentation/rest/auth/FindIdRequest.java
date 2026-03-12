package com.payvue.app.presentation.rest.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

record FindIdRequest(
    @NotBlank @Email String email
) {}
