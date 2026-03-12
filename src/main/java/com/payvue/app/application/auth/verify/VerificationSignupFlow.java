package com.payvue.app.application.auth.verify;

import com.payvue.app.domain.account.CommandType;
import com.payvue.app.domain.auth.AuthContextManager;
import com.payvue.app.domain.auth.AuthRequest;
import com.payvue.app.domain.auth.AuthResult;
import com.payvue.app.domain.auth.Authentication;
import com.payvue.app.domain.auth.CompletionStrategyClassification;
import com.payvue.app.domain.auth.RedirectCompletionResult;
import com.payvue.app.domain.auth.VerificationPurpose;
import com.payvue.app.domain.auth.VerificationTokenService;
import com.payvue.app.domain.community.CommunityCategory;
import com.payvue.app.domain.asset.OnlineCashManager;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.core.community.CommunityInitCommand;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import java.util.function.Consumer;

public final class VerificationSignupFlow {
  private final VerificationSignupContext context;

  private VerificationSignupFlow(VerificationSignupContext context) {
    this.context = context;
  }

  public static VerificationSignupFlow start(String token) {
    return new VerificationSignupFlow(new VerificationSignupContext(token));
  }

  public VerificationSignupFlow token(Consumer<VerificationSignupFlow> step) {
    step.accept(this);
    return this;
  }

  public VerificationSignupFlow auth(Consumer<VerificationSignupFlow> step) {
    step.accept(this);
    return this;
  }

  public VerificationSignupFlow user(Consumer<VerificationSignupFlow> step) {
    step.accept(this);
    return this;
  }

  public VerificationSignupFlow verify(Consumer<VerificationSignupFlow> step) {
    step.accept(this);
    return this;
  }

  public VerificationSignupFlow community(Consumer<VerificationSignupFlow> step) {
    step.accept(this);
    return this;
  }

  public VerificationSignupFlow asset(Consumer<VerificationSignupFlow> step) {
    step.accept(this);
    return this;
  }

  public VerificationSignupFlow verify(AuthContextManager authContextManager) {
    RedirectCompletionResult completion = authContextManager.authenticate(
        Authentication.EMAIL,
        CompletionStrategyClassification.REDIRECT,
        CompletionStrategyClassification.Sequence.VERIFIFY,
        AuthRequest.ofEmailVerify(context.token(), VerificationPurpose.SIGNUP)
    );
    AuthResult result = completion.result();
    if (result == null || result.isFailure() || result.user() == null) {
      throw new IllegalArgumentException(result == null ? "invalid token" : result.error().message());
    }
    context.accountView(result.user());
    return this;
  }

  public VerificationSignupFlow loadUser(UserContextManager userContextManager) {
    UserAccountView view = context.accountView();
    String email = view == null ? null : view.email();
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("유효하지 않은 인증 정보입니다.");
    }
    User user = userContextManager.findByEmail(email, User.class)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    context.user(user);
    return this;
  }

  public VerificationSignupFlow ensureNotVerified() {
    UserAccountView view = UserAccountView.from(context.user());
    if (view.emailVerified()) {
      context.alreadyVerified(true);
    }
    return this;
  }

  public VerificationSignupFlow initPersonalCommunity(CommunityContextManager communityContextManager) {
    if (context.alreadyVerified()) {
      return this;
    }
    CommunityInitCommand initCommand = new CommunityInitCommand(
        CommunityCategory.PERSONAL.name(),
        UserAccountView.from(context.user())
    );
    Community community = communityContextManager.init(initCommand);
    context.community(community);
    return this;
  }

  public VerificationSignupFlow initializeAssets(OnlineCashManager cashManager, long initialAmount) {
    if (context.alreadyVerified()) {
      return this;
    }
    Community community = context.community();
    User user = context.user();
    if (community == null || user == null) {
      throw new IllegalStateException("커뮤니티 또는 사용자 정보가 없습니다.");
    }
    cashManager.initial(community, user, initialAmount);
    return this;
  }

  public VerificationSignupFlow activateUser(UserContextManager userContextManager) {
    if (context.alreadyVerified()) {
      return this;
    }
    User user = context.user();
    user.verifyEmail();
    user.activate();
    if (context.community() != null) {
      user.joinCommunity(context.community().getId().toString());
    }
    userContextManager.save(user, CommandType.UPDATE_PROFILE::name);
    return this;
  }

  public VerificationSignupContext end() {
    return context;
  }
}
