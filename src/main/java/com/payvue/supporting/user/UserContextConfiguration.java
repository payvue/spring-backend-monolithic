package com.payvue.supporting.user;

import com.payvue.app.domain.account.CommandType;
import com.payvue.app.domain.account.UserNotificationSetting;
import com.payvue.supporting.user.command.*;
import com.payvue.app.domain.account.UserSecuritySetting;
import com.payvue.app.domain.account.UserPaymentLimitSetting;
import com.payvue.supporting.user.query.UserQueryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserContextConfiguration {

  @Bean
  public UserContextManager userContextManager(
      UserRepository userRepository,
      UserQueryRepository userQueryRepository
  ) {

    return UserContextBuilder.create()
        .delegator(new DefaultUserViewDelegator())
        .command(cmd -> cmd
            .repository(userRepository)
            .context(CommandType.REGISTER.name())
            .policy(PolicyChain.of(new PasswordPolicySpec(UserPasswordGuard::defaultPolicy)))
            .features(
                FeatureDefaultsRegistry.builder()
                    .register(UserNotificationSetting.defaults())
                    .register(UserSecuritySetting.defaults())
                    .register(UserPaymentLimitSetting.defaults())
                    .build()
            )
            .context(CommandType.CHANGE_PASSWORD.name())
            .policy(PolicyChain.of(new PasswordPolicySpec(UserPasswordGuard::defaultPolicy)))
            .context(CommandType.UPDATE_PROFILE.name())
            .policy(command -> true)
            .lookup(new UserLookupRegistry()
                    .register(UserId.class, key -> userRepository.findById(key.value()).orElse(null))
                    .register(String.class, key -> userRepository.findByEmail(key.value()).orElse(null))
                    ::lookup)
        )
        .query(q -> q
            .repository(userQueryRepository)
            .lookup(new UserLookupRegistry()
                    .register(UserId.class, key -> userRepository.findById(key.value()).map(UserAccountView::from).orElse(null))
                    .register(String.class, key -> userRepository.findByEmail(key.value()).map(UserAccountView::from).orElse(null))
                    ::lookup)
        )
        .build();
  }

  @Bean
  public UserPersistenceMapper userPersistenceMapper(ObjectMapper objectMapper) {
    return new UserPersistenceMapper(objectMapper);
  }
}
