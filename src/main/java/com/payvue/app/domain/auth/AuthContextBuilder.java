package com.payvue.app.domain.auth;

import com.payvue.supporting.user.UserContextManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

class AuthContextBuilder {
  private final Map<UnitId, AuthOrchestrator> flows = new HashMap<>();
  private UserContextManager defaultUserContext;
  private SessionManager defaultSessionManager;
  private AuthContextSpec pendingSpec;

  public static AuthContextBuilder builder() {
    return new AuthContextBuilder();
  }

  public AuthContextBuilder user(UserContextManager userContextManager) {
    this.defaultUserContext = userContextManager;
    return this;
  }

  public AuthContextBuilder session(java.util.function.Consumer<SessionConfigurer> consumer) {
    SessionConfigurer configurer = new SessionConfigurer();
    consumer.accept(configurer);
    this.defaultSessionManager = configurer.build();
    return this;
  }

  public AuthContextBuilder unit(UnitId id, Consumer<AuthContextSpec> consumer) {
    Objects.requireNonNull(id, "aggregateId is required");
    Objects.requireNonNull(consumer, "context spec is required");
    Authentication type = Authentication.valueOf(id.authentication());
    CompletionStrategyClassification completionType = CompletionStrategyClassification.valueOf(id.completion());
    AuthContextSpec spec = new AuthContextSpec(type, completionType);
    this.pendingSpec = spec;
    consumer.accept(spec);
    registerAggregate(spec);
    return this;
  }

  public AuthContextBuilder unit(String authentication, Consumer<AuthContextSpec> consumer) {
    Objects.requireNonNull(authentication, "authentication is required");
    Objects.requireNonNull(consumer, "context spec is required");
    Authentication type = Authentication.valueOf(authentication);
    AuthContextSpec spec = new AuthContextSpec(type, null);
    this.pendingSpec = spec;
    consumer.accept(spec);
    registerAggregate(spec);
    return this;
  }

  public AuthContextBuilder unit(String authentication, String strategy, Consumer<AuthContextSpec> consumer) {
    Objects.requireNonNull(authentication, "authentication is required");
    Objects.requireNonNull(strategy, "strategy is required");
    Objects.requireNonNull(consumer, "context spec is required");
    Authentication type = Authentication.valueOf(authentication);
    CompletionStrategyClassification completionType = CompletionStrategyClassification.valueOf(strategy);
    AuthContextSpec spec = new AuthContextSpec(type, completionType);
    this.pendingSpec = spec;
    consumer.accept(spec);
    registerAggregate(spec);
    return this;
  }

  public AuthContextBuilder context(Consumer<AuthContextSpec> consumer) {
    Objects.requireNonNull(consumer, "context spec is required");
    if (pendingSpec == null) {
      throw new IllegalStateException("aggregate must be set before context");
    }
    consumer.accept(pendingSpec);
    registerAggregate(pendingSpec);
    return this;
  }

  public AuthContextManager build() {
    return new DefaultAuthContextManager(Map.copyOf(flows));
  }

  private void registerAggregate(AuthContextSpec spec) {
    AuthContext context = spec.build(defaultUserContext, defaultSessionManager);
    UnitId id = new UnitId(context.type().name(), context.completionStrategyClassification().name());
    flows.put(id, new AuthOrchestrator(context));
    pendingSpec = null;
  }

  private static final class DefaultAuthContextManager implements AuthContextManager {
    private final Map<UnitId, AuthOrchestrator> flows;

    private DefaultAuthContextManager(Map<UnitId, AuthOrchestrator> flows) {
      this.flows = flows;
    }

    @Override
    public <T extends AuthCompletion> T authenticate(
        Authentication type,
        CompletionStrategyClassification strategy,
        CompletionStrategyClassification.Sequence sequence,
        AuthRequest request
    ) {
      AuthOrchestrator flow = flows.get(new UnitId(type.name(), strategy.name()));
      if (flow == null) {
        return castCompletion(strategy, CompletionStrategyClassification.failure(strategy, AuthErrorCode.UNSUPPORTED_AUTH));
      }
      if (!strategy.supports(sequence)) {
        return castCompletion(strategy, CompletionStrategyClassification.failure(strategy, AuthErrorCode.INVALID_REQUEST));
      }
      AuthCompletion completion = flow.execute(request, sequence);
      return castCompletion(strategy, completion);
    }

    @Override
    public <T extends AuthCompletion> T deauthenticate(AuthRequest request) {

      Authentication type = Authentication.NONE;
      CompletionStrategyClassification strategy = CompletionStrategyClassification.DIRECT;
      CompletionStrategyClassification.Sequence sequence = CompletionStrategyClassification.Sequence.VERIFIFY;

      AuthOrchestrator flow = flows.get(new UnitId(type.name(), strategy.name()));

      if (flow == null) {
        return castCompletion(strategy, CompletionStrategyClassification.failure(strategy, AuthErrorCode.UNSUPPORTED_AUTH));
      }
      if (!strategy.supports(sequence)) {
        return castCompletion(strategy, CompletionStrategyClassification.failure(strategy, AuthErrorCode.INVALID_REQUEST));
      }
      AuthCompletion completion = flow.execute(request, sequence);
      return castCompletion(strategy, completion);
    }
  }

  @SuppressWarnings("unchecked")
  private static <T extends AuthCompletion> T castCompletion(
      CompletionStrategyClassification strategy,
      AuthCompletion completion
  ) {
    if (!strategy.resultType().isInstance(completion)) {
      throw new IllegalStateException("completion result type mismatch");
    }
    return (T) completion;
  }
}
