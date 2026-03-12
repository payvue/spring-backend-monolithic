package com.payvue.app.domain.community;

import com.payvue.core.community.Community;
import com.payvue.core.community.RoleDescriptor;

import java.util.UUID;

public class Personal extends Community {

    private static final int MAX_MEMBERS = 1;

    private Personal(String name, String description, UUID ownerId, RoleDescriptor adminRole) {
        super(name, description, ownerId, adminRole);
        validateInvariants();
    }

    public static Personal create(String name, String description, UUID ownerId, RoleDescriptor adminRole) {
        return new Personal(name, description, ownerId, adminRole);
    }

    @Override
    public String getCommunityType() {
        return CommunityCategory.PERSONAL.name();
    }


    public int getMaxMembers() {
        return MAX_MEMBERS;
    }

    @Override
    protected void validateInvariants() {
        super.validateInvariants();
        if (getParticipantCount() > MAX_MEMBERS) {
            throw new IllegalStateException(
                    "개인 커뮤니티는 최대 " + MAX_MEMBERS + "명까지만 허용됩니다."
            );
        }
    }
}
