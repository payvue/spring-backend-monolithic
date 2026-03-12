package com.payvue.app.presentation.rest.auth;

import com.payvue.app.presentation.rest.ApiResponse;
import com.payvue.app.presentation.rest.auth.FindIdRequest;
import com.payvue.app.presentation.rest.auth.FindPasswordRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Auth", description = "Account recovery endpoints")
interface AccountRecoveryControllerDocs {

  @Operation(summary = "Request ID recovery", description = "Request ID recovery email")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Request accepted")
  })
  ResponseEntity<ApiResponse<Void>> requestFindId(
      @Parameter(description = "계정 식별 정보")
      @jakarta.validation.Valid @RequestBody FindIdRequest request
  );

  @Operation(summary = "Request password reset", description = "Request password reset email")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Request accepted")
  })
  ResponseEntity<ApiResponse<Void>> requestResetPassword(
      @Parameter(description = "계정 식별 정보")
      @jakarta.validation.Valid @RequestBody FindPasswordRequest request
  );

}
