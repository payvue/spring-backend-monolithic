package com.payvue.core.community;

import com.payvue.app.domain.community.CommunityCategory;
import com.payvue.app.domain.community.Cooperative;
import com.payvue.app.domain.community.Personal;
import com.payvue.app.domain.community.Role;
import com.payvue.app.domain.community.RoleCatalog;
import com.payvue.supporting.user.command.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class CommunityContextConfiguration {

  @Bean
  public CommunityContextManager communityContext(
      CommunityRepository communityRepository,
      UserRepository userRepository,
      RoleCatalog roleCatalog
  ) {
    return CommunityContextBuilder.create()
        .repository(communityRepository)
        .context(ctx -> ctx
            .category(CommunityCategory.PERSONAL.name())
            .implement(Personal.class)
            .initialize(new PersonalCommunityInitializer(roleCatalog))
        )
        .context(ctx -> ctx
            .category(CommunityCategory.COOPERATIVE.name())
            .implement(Cooperative.class)
            .initialize(new CooperativeCommunityInitializer(roleCatalog))
        )
        .resolver(new MemberShipTranslator(userRepository))
        .build();
  }


  @Bean
  public RoleCatalog roleCatalog() {
    return RoleCatalog.builder()
        .register(Role.of("ADMIN", "관리자", Set.of(Role.Permission.ALL)))
        .register(Role.of("MEMBER", "멤버", Set.of(Role.Permission.READ, Role.Permission.REQUEST)))
        .register(Role.of("GUEST", "게스트", Set.of(Role.Permission.READ)))
        .build();
  }
}
