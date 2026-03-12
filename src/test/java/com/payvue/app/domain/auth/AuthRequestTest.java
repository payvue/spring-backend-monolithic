package com.payvue.app.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthRequestTest {

  @Test
  @DisplayName("비밀번호 인증 요청은 식별자와 비밀값을 포함한다")
  void ofPasswordCreatesRequest() {
    AuthRequest request = AuthRequest.ofPassword("a@b.com", "pw");

    assertThat(request.identifier()).isEqualTo("a@b.com");
    assertThat(request.secret()).isEqualTo("pw");
    assertThat(request.purpose()).isNull();
  }

  @Test
  @DisplayName("이메일 인증 요청은 목적을 포함한다")
  void ofEmailIssueCreatesRequest() {
    AuthRequest request = AuthRequest.ofEmailIssue("a@b.com", VerificationPurpose.SIGNUP);

    assertThat(request.identifier()).isEqualTo("a@b.com");
    assertThat(request.secret()).isNull();
    assertThat(request.purpose()).isEqualTo(VerificationPurpose.SIGNUP);
  }

  @Test
  @DisplayName("이메일 검증 요청은 토큰과 목적을 포함한다")
  void ofEmailVerifyCreatesRequest() {
    AuthRequest request = AuthRequest.ofEmailVerify("token", VerificationPurpose.SIGNUP);

    assertThat(request.identifier()).isNull();
    assertThat(request.secret()).isEqualTo("token");
    assertThat(request.purpose()).isEqualTo(VerificationPurpose.SIGNUP);
  }

  @Test
  @DisplayName("OAuth 요청은 provider 정보로 secret을 생성한다")
  void ofOAuthCreatesRequest() {
    AuthRequest request = AuthRequest.ofOAuth("google", "id-1", "a@b.com");

    assertThat(request.identifier()).isEqualTo("a@b.com");
    assertThat(request.secret()).isEqualTo("google:id-1");
  }

  @Test
  @DisplayName("필수 값이 없으면 예외가 발생한다")
  void factoryRequiresArguments() {
    assertThatThrownBy(() -> AuthRequest.ofPassword(null, "pw"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> AuthRequest.ofEmailIssue("a@b.com", null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> AuthRequest.ofEmailVerify("t", null))
        .isInstanceOf(NullPointerException.class);
  }
}
