package com.payvue.app.presentation.rest.account;

import com.payvue.app.application.account.create.AccountCreateCommand;
import com.payvue.app.application.account.edit.AccountEditCommand;
import com.payvue.app.application.account.query.AccountExistCommand;
import com.payvue.app.application.account.query.AccountExistResult;
import com.payvue.app.application.account.query.AccountInfoResult;
import com.payvue.app.application.account.AccountUseCase;
import com.payvue.app.application.account.edit.ChangePasswordCommand;
import com.payvue.app.presentation.rest.ApiResponse;
import com.payvue.app.presentation.rest.ResponseCode;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import com.payvue.supporting.user.command.UserRepository;

import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/account")
@RequiredArgsConstructor
class AccountController implements AccountControllerDocs {

  private final AccountUseCase accountUseCase;
  private final UserRepository userRepository;

  @PostMapping("/create")
  public ApiResponse<String> create(@RequestBody AccountCreateRequest request) {
    accountUseCase.create(new AccountCreateCommand(
        request.alias(),
        request.username(),
        request.name(),
        request.residentRegistrationNo(),
        request.birthYear(),
        request.birthMonth(),
        request.birthDay(),
        request.email(),
        request.phone(),
        request.password(),
        request.passwordConfirm(),
        request.sido(),
        request.sigungu(),
        request.roadAddress(),
        request.postalCode(),
        request.detailAddress()
    ));

    return ApiResponse.success("인증 메일이 발송되었습니다.");
  }

  @PutMapping("/edit")
  public ApiResponse<Void> edit(
      @AuthenticationPrincipal UserDetails user,
      @RequestBody AccountEditRequest request
  ) {
    accountUseCase.edit(user, new AccountEditCommand(request.name(), request.contact()));
    return ApiResponse.success();
  }

  @PutMapping("/change-pw")
  public ApiResponse<Void> changePassword(
      @AuthenticationPrincipal UserDetails user,
      @RequestBody ChangePasswordRequest request
  ) {
    accountUseCase.changePassword(
        user,
        new ChangePasswordCommand(request.currentPassword(), request.newPassword())
    );
    return ApiResponse.success();
  }

  @PostMapping("/exist")
  public ApiResponse<AccountExistResponse> exist(@RequestBody AccountExistRequest request) {
    AccountExistResult result = accountUseCase.exist(new AccountExistCommand(request.email()));
    AccountExistResponse.Contact contact = null;
    if (result != null && result.contact() != null) {
      contact = new AccountExistResponse.Contact(
          result.contact().email(),
          result.contact().phone()
      );
    }
    return ApiResponse.success(new AccountExistResponse(result == null ? null : result.name(), contact));
  }

  @GetMapping("/info")
  public ApiResponse<AccountInfoResponse> info(@AuthenticationPrincipal UserDetails user) {
    AccountInfoResult result = accountUseCase.info(user);
    return ApiResponse.success(AccountInfoResponse.from(result));
  }

  @GetMapping("/profile")
  public ApiResponse<UserProfileResponse> profile(@RequestParam String email) {

    User user = userRepository.findByEmail(email.trim()).orElse(null);
    if (user == null) {
      return ApiResponse.of(ResponseCode.NOT_FOUND, "유저를 찾을 수 없습니다.", null);
    }
    UserAccountView view = UserAccountView.from(user);
    UserProfileResponse response = new UserProfileResponse(
        user.getId().toString(),
        view.displayName(),
        view.email(),
        null,
        Objects.requireNonNull(view.status()).name()
    );

    return ApiResponse.success(response);
  }
}
