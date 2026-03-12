package com.payvue.core.community;

import com.payvue.app.domain.community.Personal;
import com.payvue.app.domain.community.RoleCatalog;
import com.payvue.supporting.user.command.UserAccountView;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
class PersonalCommunityInitializer implements CommunityInitializer {
    private final RoleCatalog roleCatalog;

    @Transactional
    @Override
    public Personal initialize(CommunityInitCommand command) {
        UserAccountView view = command.view();
        String displayName = view.displayName() == null ? "사용자" : view.displayName();
        String communityName = displayName + "의 지갑";
        return Personal.create(
                communityName,
                "개인 자산 관리 커뮤니티",
                view.userId().getValue(),
                roleCatalog.admin()
        );
    }

    @Override
    public String category() {
        return com.payvue.app.domain.community.CommunityCategory.PERSONAL.name();
    }
}
