package com.payvue.app.domain.community;

import com.payvue.core.community.Community;
import com.payvue.core.community.RoleDescriptor;
import com.payvue.supporting.interaction.Interaction;
import com.payvue.supporting.user.command.User;

public interface MemberManager {
    Interaction invite(Community community, User initiator, User invitee);
    Interaction changeRole(Community community, User initiator, User target, RoleDescriptor newRole);
}
