package com.payvue.app.domain.community;

import com.payvue.core.community.Community;
import com.payvue.core.community.Participant;
import com.payvue.core.community.RoleDescriptor;
import com.payvue.supporting.interaction.ChangeRoleDecision;
import com.payvue.supporting.interaction.Interaction;
import com.payvue.supporting.interaction.InteractionExecutor;
import com.payvue.supporting.interaction.InviteDecision;
import com.payvue.supporting.interaction.ManageIntent;
import com.payvue.supporting.user.command.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberManagerImpl implements MemberManager{

  private final InteractionExecutor interactionExecutor;
  private final RoleCatalog roleCatalog;

  public Interaction invite(Community community, User initiator, User invitee) {
    return interactionExecutor.executeInternal(new InviteDecision(
            new ManageIntent(
                    to(initiator, roleCatalog.admin()),
                    to(invitee, roleCatalog.member())
            )), community
    );
  }

  public Interaction changeRole(Community community, User initiator, User target, RoleDescriptor newRole) {
    Participant actor = to(initiator, roleCatalog.admin());
    Participant participant = to(target, roleCatalog.member());
    ChangeRoleDecision decision = new ChangeRoleDecision(
            new ManageIntent(actor, participant),
            newRole
    );
    return interactionExecutor.executeInternal(decision, community);
  }

  private Participant to(User user, RoleDescriptor role) {
    if (user == null || user.getId() == null) {
      throw new IllegalArgumentException("사용자 정보가 없습니다.");
    }
    return Participant.of(user.getId().getValue(), role);
  }
}
