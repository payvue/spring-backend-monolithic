package com.payvue.app.presentation.rest.auth;

import com.payvue.app.application.auth.AuthUseCase;
import com.payvue.app.application.auth.login.LoginCommand;
import com.payvue.app.application.auth.login.LoginResult;
import com.payvue.app.application.auth.login.LogoutCommand;
import com.payvue.app.infrastructure.security.TokenHeaderResolver;
import com.payvue.app.presentation.rest.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/auth")
@RequiredArgsConstructor
class AuthController implements AuthControllerDocs {

  private final AuthUseCase authUseCase;
  private final TokenHeaderResolver tokenHeaderResolver;

  @PostMapping("/login")
  public ApiResponse<LoginResponse> login(@jakarta.validation.Valid @RequestBody LoginRequest request) {
    LoginResult result = authUseCase.login(new LoginCommand(request.email(), request.password()));
    return ApiResponse.success(new LoginResponse(result.accessToken()));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(
      @AuthenticationPrincipal UserDetails user,
      HttpServletRequest request
  ) {
    authUseCase.logout(user, new LogoutCommand(tokenHeaderResolver.resolve(request)));
    return ApiResponse.success();
  }
}
