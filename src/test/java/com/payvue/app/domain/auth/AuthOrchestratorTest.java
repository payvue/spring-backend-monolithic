package com.payvue.app.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthOrchestratorTest {

  @Test
  @DisplayName("요청이나 시퀀스가 없으면 INVALID_REQUEST를 반환한다")
  void executeReturnsInvalidRequestWhenMissingInput() {
    AuthContext context = new AuthContext(
        Authentication.PASSWORD,
        CompletionStrategyClassification.DIRECT,
        null,
        new StubSessionManager(),
        new DirectCompletion(),
        Map.of()
    );
    AuthOrchestrator orchestrator = new AuthOrchestrator(context);

    AuthCompletion missingRequest = orchestrator.execute(null, CompletionStrategyClassification.Sequence.VERIFIFY);
    AuthCompletion missingSequence = orchestrator.execute(AuthRequest.ofLogout("a@b.com"), null);

    assertThat(missingRequest.result().error().code()).isEqualTo(AuthErrorCode.INVALID_REQUEST);
    assertThat(missingSequence.result().error().code()).isEqualTo(AuthErrorCode.INVALID_REQUEST);
  }

  @Test
  @DisplayName("성공 결과는 세션이 유지되고 meta에 포함된다")
  void executeMaintainsSessionOnSuccess() {
    StubSessionManager sessionManager = new StubSessionManager();
    AuthSequenceHandler handler = (request, ctx) -> AuthResult.success(UserAccountView.from(newUser()));
    AuthContext context = new AuthContext(
        Authentication.PASSWORD,
        CompletionStrategyClassification.DIRECT,
        null,
        sessionManager,
        new DirectCompletion(),
        Map.of(CompletionStrategyClassification.Sequence.VERIFIFY, handler)
    );
    AuthOrchestrator orchestrator = new AuthOrchestrator(context);

    AuthCompletion completion = orchestrator.execute(
        AuthRequest.ofPassword("a@b.com", "pw"),
        CompletionStrategyClassification.Sequence.VERIFIFY
    );

    assertThat(sessionManager.maintainCalled).isTrue();
    assertThat(completion.result().meta()).containsKey(SessionManager.SESSION_META_KEY);
  }

  @Test
  @DisplayName("실패 결과는 세션이 중단된다")
  void executeSuspendsSessionOnFailure() {
    StubSessionManager sessionManager = new StubSessionManager();
    AuthSequenceHandler handler = (request, ctx) -> AuthResult.failure(AuthErrorCode.INVALID_REQUEST);
    AuthContext context = new AuthContext(
        Authentication.PASSWORD,
        CompletionStrategyClassification.DIRECT,
        null,
        sessionManager,
        new DirectCompletion(),
        Map.of(CompletionStrategyClassification.Sequence.VERIFIFY, handler)
    );
    AuthOrchestrator orchestrator = new AuthOrchestrator(context);

    AuthCompletion completion = orchestrator.execute(
        AuthRequest.ofPassword("a@b.com", "pw"),
        CompletionStrategyClassification.Sequence.VERIFIFY
    );

    assertThat(completion.result().error().code()).isEqualTo(AuthErrorCode.INVALID_REQUEST);
    assertThat(sessionManager.suspendCalled).isTrue();
  }

  private User newUser() {
    return User.create(
        "login",
        "a@b.com",
        "010",
        "nick",
        "real",
        LocalDate.now(),
        "sido",
        "sigungu",
        "road",
        "detail",
        "12345"
    );
  }

  static final class StubSessionManager implements SessionManager {
    boolean maintainCalled;
    boolean suspendCalled;

    @Override
    public AuthSession maintain(UserAccountView user) {
      maintainCalled = true;
      return new AuthSession("access", "refresh");
    }

    @Override
    public void suspend(UserAccountView user) {
      suspendCalled = true;
    }
  }
}
