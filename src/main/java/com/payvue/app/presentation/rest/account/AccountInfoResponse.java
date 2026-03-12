package com.payvue.app.presentation.rest.account;

import com.payvue.app.application.account.query.AccountInfoResult;
import io.swagger.v3.oas.annotations.media.Schema;

record AccountInfoResponse(
    @Schema(description = "계정 정보")
    Account account,
    @Schema(description = "사용자 정보")
    Info info,
    @Schema(description = "자산 정보")
    Asset asset
) {
  static AccountInfoResponse from(AccountInfoResult result) {
    Account account = null;
    Info info = null;
    Asset asset = null;
    if (result != null) {
      if (result.account() != null) {
        Status status = null;
        if (result.account().status() != null) {
          status = new Status(
              result.account().status().lastLoggedIn(),
              result.account().status().joinAt()
          );
        }
        account = new Account(
            result.account().alias(),
            result.account().id(),
            status
        );
      }
      if (result.info() != null) {
        Contact contact = null;
        if (result.info().contact() != null) {
          contact = new Contact(
              result.info().contact().email(),
              result.info().contact().phone()
          );
        }
        info = new Info(
            result.info().name(),
            contact,
            result.info().avatar()
        );
      }
      if (result.asset() != null) {
        asset = new Asset(
            result.asset().balance(),
            result.asset().point()
        );
      }
    }
    return new AccountInfoResponse(account, info, asset);
  }

  record Account(
      @Schema(description = "별명", example = "별명")
      String alias,
      @Schema(description = "계정 ID", example = "user-id")
      String id,
      @Schema(description = "상태 정보")
      Status status
  ) {}

  record Status(
      @Schema(description = "마지막 로그인", example = "2024-01-01")
      String lastLoggedIn,
      @Schema(description = "가입일", example = "2024-01-01")
      String joinAt
  ) {}

  record Info(
      @Schema(description = "이름", example = "홍길동")
      String name,
      @Schema(description = "연락처")
      Contact contact,
      @Schema(description = "아바타 URL", example = "https://example.com/avatar.png")
      String avatar
  ) {}

  record Contact(
      @Schema(description = "이메일", example = "user@example.com")
      String email,
      @Schema(description = "전화번호", example = "01012341234")
      String phone
  ) {}

  record Asset(
      @Schema(description = "잔액", example = "1000")
      long balance,
      @Schema(description = "포인트", example = "50")
      long point
  ) {}
}
