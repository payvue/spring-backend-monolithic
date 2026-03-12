package com.payvue.app.domain.community;

import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityId;
import com.payvue.core.community.RoleDescriptor;
import java.util.UUID;

public final class SystemCommunity extends Community {

    public static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    public static final CommunityId SYSTEM_COMMUNITY_ID =
            CommunityId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    private SystemCommunity(String name, String description, RoleDescriptor adminRole) {
        super(SYSTEM_COMMUNITY_ID, name, description, SYSTEM_USER_ID, adminRole);
    }

    public static SystemCommunity create(RoleDescriptor adminRole) {
        return new SystemCommunity("SYSTEM", "System Community", adminRole);
    }

    @Override
    public String getCommunityType() {
        return CommunityCategory.SYSTEM.name();
    }
}
