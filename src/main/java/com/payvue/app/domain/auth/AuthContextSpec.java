package com.payvue.app.domain.auth;

import com.payvue.supporting.user.UserContextManager;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

class AuthContextSpec {
  private final Authentication type;
  private CompletionStrategyClassification completionStrategyClassification;
  private CompletionStrategy completionStrategy;
  private UserContextManager userContextManager;
  private SessionManager sessionManager;
  private final Map<CompletionStrategyClassification.Sequence, AuthSequenceHandler> handlers =
      new EnumMap<>(CompletionStrategyClassification.Sequence.class);

  AuthContextSpec(Authentication type, CompletionStrategyClassification completionStrategyClassification) {
    this.type = type;
    this.completionStrategyClassification = completionStrategyClassification;
  }

  public AuthContextSpec user(UserContextManager userContextManager) {
    this.userContextManager = userContextManager;
    return this;
  }

  public AuthContextSpec session(SessionManager sessionManager) {
    this.sessionManager = sessionManager;
    return this;
  }

  public AuthContextSpec session(java.util.function.Consumer<SessionConfigurer> consumer) {
    SessionConfigurer configurer = new SessionConfigurer();
    consumer.accept(configurer);
    this.sessionManager = configurer.build();
    return this;
  }

  public AuthContextSpec handler(CompletionStrategyClassification.Sequence sequence, AuthSequenceHandler handler) {
    handlers.put(sequence, handler);
    return this;
  }

  public AuthContextSpec strategy(CompletionStrategy completionStrategy) {
    this.completionStrategy = completionStrategy;
    if (completionStrategyClassification == null) {
      completionStrategyClassification = completionStrategy.type();
    }
    return this;
  }

  public AuthContextSpec strategy(String strategyName, Supplier<CompletionStrategy> completionStrategySupplier) {
    CompletionStrategy strategy = completionStrategySupplier.get();
    CompletionStrategyClassification declaredType = CompletionStrategyClassification.valueOf(strategyName);
    if (strategy.type() != declaredType) {
      throw new IllegalArgumentException("strategy type mismatch");
    }
    this.completionStrategy = strategy;
    this.completionStrategyClassification = declaredType;
    return this;
  }

  public AuthContextSpec strategy(String strategyName, Consumer<StrategyConfigurer> consumer) {
    StrategyConfigurer configurer = new StrategyConfigurer();
    consumer.accept(configurer);
    CompletionStrategy strategy = configurer.build();
    CompletionStrategyClassification declaredType = CompletionStrategyClassification.valueOf(strategyName);
    if (strategy.type() != declaredType) {
      throw new IllegalArgumentException("strategy type mismatch");
    }
    this.completionStrategy = strategy;
    this.completionStrategyClassification = declaredType;
    return this;
  }

  AuthContext build(UserContextManager defaultUserContext, SessionManager defaultSessionManager) {
    if (userContextManager == null) {
      userContextManager = defaultUserContext;
    }
    if (sessionManager == null) {
      sessionManager = defaultSessionManager;
    }
    if (completionStrategy == null) {
      throw new IllegalStateException("completionStrategy is required");
    }
    return new AuthContext(
        type,
        completionStrategyClassification,
        userContextManager,
        sessionManager,
        completionStrategy,
        Map.copyOf(handlers)
    );
  }
}
