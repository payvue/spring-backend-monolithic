package com.payvue.app.domain.auth;

import com.payvue.supporting.user.UserContextManager;
import com.payvue.app.domain.security.SessionTokenRepository;
import com.payvue.app.domain.security.TokenProvider;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties(VerificationProperties.class)
public class AuthContextConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

  @Bean
  public VerificationTokenService verificationTokenService(
          VerificationTokenRepository verificationTokenRepository,
          VerificationProperties verificationProperties
  ) {
    return new VerificationTokenService(verificationTokenRepository, verificationProperties);
  }

  @Bean
  public AuthContextManager authContextManager(
      VerificationTokenService verificationTokenService,
      UserContextManager userContextManager,
      TokenProvider tokenProvider,
      PasswordEncoder passwordEncoder,
      SessionTokenRepository sessionTokenRepository
  ) {
    PasswordHasher passwordHasher = new PasswordHasherAdapter(passwordEncoder);
    AuthService authService = new AuthService(passwordHasher);

    return AuthContextBuilder.builder()
        .user(userContextManager)
        .session(s -> s
            .repository(sessionTokenRepository)
            .provider(tokenProvider)
        )
        .unit(Authentication.NONE.name(), CompletionStrategyClassification.DIRECT.name(),
                ctx -> ctx
                        .handler(CompletionStrategyClassification.Sequence.VERIFIFY,
                                new NoneAuthVerifier()::handle)
                        .strategy(CompletionStrategyClassification.DIRECT.name(),
                                strategy -> strategy.complete(new DirectCompletion()).next(new AuthenticateComplete()))
        )
        .unit(Authentication.PASSWORD.name(), CompletionStrategyClassification.DIRECT.name(),
            ctx -> ctx
                .handler(CompletionStrategyClassification.Sequence.VERIFIFY,
                    new PasswordAuthVerifier(authService)::handle)
                .strategy(CompletionStrategyClassification.DIRECT.name(),
                    strategy -> strategy.complete(new DirectCompletion()).next(new AuthenticateComplete()))
        )
        .unit(Authentication.EMAIL.name(), CompletionStrategyClassification.REDIRECT.name(),
            ctx -> ctx
                .handler(CompletionStrategyClassification.Sequence.ISSUE, new EmailTokenIssuer(verificationTokenService)::handle)
                .handler(CompletionStrategyClassification.Sequence.VERIFIFY, new EmailTokenVerifier(verificationTokenService)::handle)
                .strategy(CompletionStrategyClassification.REDIRECT.name(),
                    strategy -> strategy.complete(new RedirectCompletion()).next(new SequenceByNextStep(
                            CompletionStrategyClassification.Sequence.ISSUE,
                            new RedirectRequired()
                        ))
                )
        )

        .unit(Authentication.OAUTH2.name(), CompletionStrategyClassification.OAUTH_DYNAMIC.name(),
            ctx -> ctx
                    .handler(CompletionStrategyClassification.Sequence.VERIFIFY,
                        new OAuthAuthVerifier(userContextManager)::handle)
                    .strategy(CompletionStrategyClassification.OAUTH_DYNAMIC.name(),
                        strategy -> strategy.complete(new ConditionCompletionStrategy(CompletionStrategyClassification.OAUTH_DYNAMIC,
                                List.of(
                                    new ConditionCompletionStrategy.CompletionRule(
                                        (ctx0, result) -> result != null && result.isFailure(),
                                        new NextStepCompletionStrategy(new RedirectCompletion(), new RedirectRequired())
                                    ),
                                    new ConditionCompletionStrategy.CompletionRule(
                                        (ctx0, result) -> result != null && result.success(),
                                        new DirectCompletion()
                                    )
                                )
                            )
                        ).next(new AuthenticateComplete()))
        )
        .build();
  }
}
