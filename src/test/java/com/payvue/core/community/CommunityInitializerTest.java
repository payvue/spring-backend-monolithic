package com.payvue.core.community;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.app.domain.community.CommunityCategory;
import com.payvue.app.domain.community.Cooperative;
import com.payvue.app.domain.community.Personal;
import com.payvue.app.domain.community.Role;
import com.payvue.app.domain.community.RoleCatalog;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommunityInitializerTest {

  @Test
  @DisplayName("개인 커뮤니티 초기화는 사용자 이름 기반으로 생성된다")
  void personalInitializerCreatesCommunity() {
    RoleCatalog roleCatalog = RoleCatalog.builder()
        .register(Role.ADMIN)
        .build();
    PersonalCommunityInitializer initializer = new PersonalCommunityInitializer(roleCatalog);
    User user = newUser();
    UserAccountView view = UserAccountView.from(user);

    Personal community = initializer.initialize(new CommunityInitCommand(CommunityCategory.PERSONAL.name(), view));

    assertThat(community.getCommunityType()).isEqualTo(CommunityCategory.PERSONAL.name());
    assertThat(community.getName()).contains(view.displayName());
    assertThat(community.isAdmin(view.userId().getValue())).isTrue();
  }

  @Test
  @DisplayName("조직 커뮤니티 초기화는 사용자 이름 기반으로 생성된다")
  void organizationInitializerCreatesCommunity() {
    RoleCatalog roleCatalog = RoleCatalog.builder()
        .register(Role.ADMIN)
        .register(Role.MEMBER)
        .build();
    CooperativeCommunityInitializer initializer = new CooperativeCommunityInitializer(roleCatalog);
    User user = newUser();
    UserAccountView view = UserAccountView.from(user);

    Cooperative community = initializer.initialize(new CommunityInitCommand(CommunityCategory.COOPERATIVE.name(), view));

    assertThat(community.getCommunityType()).isEqualTo(CommunityCategory.COOPERATIVE.name());
    assertThat(community.getName()).contains(view.displayName());
    assertThat(community.isAdmin(view.userId().getValue())).isTrue();
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
}
