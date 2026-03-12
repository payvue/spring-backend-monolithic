package com.payvue.app.presentation.rest.auth;

import com.payvue.app.domain.auth.VerificationPurpose;
import com.payvue.app.presentation.rest.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Auth", description = "Unified verification endpoints")
interface VerificationControllerDocs {

    @Operation(summary = "Verify token", description = "Verify token by purpose")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Verification result")
    })
    ResponseEntity<ApiResponse<?>> verify(
      @Parameter(description = "인증 토큰", example = "token")
      @RequestParam String token,
      @Parameter(description = "인증 목적", example = "SIGNUP")
      @RequestParam VerificationPurpose purpose
    );

    @Operation(summary = "Resend verification", description = "Resend verification email")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Verification email resent")
    })
    ResponseEntity<ApiResponse<Void>> resendVerification(
            @Parameter(description = "이메일", example = "user@example.com")
            @RequestParam String email
    );
}
