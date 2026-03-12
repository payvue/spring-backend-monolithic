package com.payvue.app.presentation.rest.account;

import io.swagger.v3.oas.annotations.media.Schema;

record AccountExistResponse(
    @Schema(description = "이름", example = "홍길동")
    String name,
    @Schema(description = "연락처 정보")
    Contact contact
) {

  record Contact(
      @Schema(description = "이메일", example = "user@example.com")
      String email,
      @Schema(description = "전화번호", example = "01012341234")
      String phone
  ) {}
}
