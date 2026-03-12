package com.payvue.app.presentation.rest.account;

import io.swagger.v3.oas.annotations.media.Schema;

record AccountCreateRequest(
    @Schema(description = "별명", example = "별명")
    String alias,
    @Schema(description = "아이디", example = "login")
    String username,
    @Schema(description = "이름", example = "홍길동")
    String name,
    @Schema(description = "주민등록번호", example = "901010-1234567")
    String residentRegistrationNo,
    @Schema(description = "출생 연도", example = "1990")
    int birthYear,
    @Schema(description = "출생 월", example = "1")
    int birthMonth,
    @Schema(description = "출생 일", example = "1")
    int birthDay,
    @Schema(description = "이메일", example = "user@example.com")
    String email,
    @Schema(description = "전화번호", example = "01012341234")
    String phone,
    @Schema(description = "비밀번호", example = "Passw0rd!")
    String password,
    @Schema(description = "비밀번호 확인", example = "Passw0rd!")
    String passwordConfirm,
    @Schema(description = "시/도 코드", example = "11")
    String sido,
    @Schema(description = "구/군 코드", example = "110")
    String sigungu,
    @Schema(description = "도로명 주소", example = "테스트로")
    String roadAddress,
    @Schema(description = "우편번호", example = "12345")
    String postalCode,
    @Schema(description = "상세 주소", example = "101")
    String detailAddress
) {}
