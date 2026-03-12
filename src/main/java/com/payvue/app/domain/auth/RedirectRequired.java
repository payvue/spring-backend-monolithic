package com.payvue.app.domain.auth;

final class RedirectRequired implements NextStep {
  static final String REDIRECT_URL = "redirectUrl";

  @Override
  public AuthCompletion apply(AuthResult result, AuthContext context, CompletionStrategy delegate) {
    if(!result.meta().containsKey(REDIRECT_URL)) {
      throw new IllegalStateException("다음 스탭을 실행하기 위한 필수 키가 설정되지 않았습니다.");
    }

    return delegate.complete(context, result);
  }
}
