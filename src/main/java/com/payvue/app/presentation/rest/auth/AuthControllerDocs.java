package com.payvue.app.presentation.rest.auth;

import com.payvue.app.presentation.rest.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Auth", description = "Authentication endpoints")
interface AuthControllerDocs {

  @Operation(summary = "Login", description = "Authenticate user and return access token")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login success")
  })
  ApiResponse<LoginResponse> login(@jakarta.validation.Valid @RequestBody LoginRequest request);

  @Operation(summary = "Logout", description = "Invalidate current access token")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logout success")
  })
  ApiResponse<Void> logout(
      @AuthenticationPrincipal UserDetails user,
      HttpServletRequest request
  );
}
