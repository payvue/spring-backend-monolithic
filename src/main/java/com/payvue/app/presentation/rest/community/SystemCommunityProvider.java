package com.payvue.app.presentation.rest.community;

import com.payvue.app.domain.community.RoleCatalog;
import com.payvue.app.domain.community.SystemCommunity;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SystemCommunityProvider {

    private final CommunityRepository communityRepository;
    private final RoleCatalog roleCatalog;

    public Community getOrCreate() {
        return communityRepository.findById(SystemCommunity.SYSTEM_COMMUNITY_ID)
            .orElseGet(() -> communityRepository.save(SystemCommunity.create(roleCatalog.admin())));
    }

    public Community ensure() {
        return getOrCreate();
    }

    public String systemId() {
        return SystemCommunity.SYSTEM_COMMUNITY_ID.toString();
    }

    public boolean isSystemCommunity(String communityId) {
        if (communityId == null || communityId.isBlank()) {
            return false;
        }
        return Objects.equals(communityId, SystemCommunity.SYSTEM_COMMUNITY_ID.toString());
    }
}
