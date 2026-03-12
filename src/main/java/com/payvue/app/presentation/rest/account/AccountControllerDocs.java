package com.payvue.app.presentation.rest.account;

import com.payvue.app.presentation.rest.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Account", description = "Account endpoints")
interface AccountControllerDocs {

  @Operation(summary = "Create account", description = "Create account and send verification email")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account created")
  })
  ApiResponse<String> create(@RequestBody AccountCreateRequest request);

  @Operation(summary = "Edit account", description = "Update account profile")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account updated")
  })
  ApiResponse<Void> edit(
      @AuthenticationPrincipal UserDetails user,
      @RequestBody AccountEditRequest request
  );

  @Operation(summary = "Change password", description = "Change account password")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password changed")
  })
  ApiResponse<Void> changePassword(
      @AuthenticationPrincipal UserDetails user,
      @RequestBody ChangePasswordRequest request
  );

  @Operation(summary = "Check account", description = "Check account existence by email")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account existence result")
  })
  ApiResponse<AccountExistResponse> exist(@RequestBody AccountExistRequest request);

  @Operation(summary = "Account info", description = "Get account information")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account info")
  })
  ApiResponse<AccountInfoResponse> info(@AuthenticationPrincipal UserDetails user);
}
