package com.payvue.core.community;

import com.payvue.app.domain.community.Cooperative;
import com.payvue.app.domain.community.RoleCatalog;
import com.payvue.supporting.user.command.UserAccountView;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
class CooperativeCommunityInitializer implements CommunityInitializer {
  private final RoleCatalog roleCatalog;

  @Override
  @Transactional
  public Cooperative initialize(CommunityInitCommand command) {
    UserAccountView view = command.view();
    String displayName = view.displayName() == null ? "사용자" : view.displayName();
    String communityName = displayName + "의 그룹";
    return Cooperative.create(
        communityName,
        "기본 조직 커뮤니티",
        view.userId().getValue(),
        roleCatalog.admin(),
        roleCatalog.member()
    );
  }

  @Override
  public String category() {
    return com.payvue.app.domain.community.CommunityCategory.COOPERATIVE.name();
  }
}
