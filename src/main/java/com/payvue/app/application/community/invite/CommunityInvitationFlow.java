package com.payvue.app.application.community.invite;

import com.payvue.app.domain.community.MemberManager;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityId;
import com.payvue.core.community.CommunityRepository;
import com.payvue.generic.alarm.AlarmContextManager;
import com.payvue.generic.alarm.AlarmFactory;
import com.payvue.generic.alarm.Trigger;
import com.payvue.supporting.interaction.Interaction;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.UserAccountView;
import com.payvue.supporting.user.command.User;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.security.core.userdetails.UserDetails;

public final class CommunityInvitationFlow {
  private final CommunityInvitation context;

  private CommunityInvitationFlow(CommunityInvitation context) {
    this.context = context;
  }

  public static CommunityInvitationFlow start(
      UserDetails userDetails,
      UUID communityId,
      List<String> emails
  ) {
    return new CommunityInvitationFlow(new CommunityInvitation(userDetails, communityId, emails));
  }

  public CommunityInvitationFlow user(Consumer<CommunityInvitationFlow> step) {
    step.accept(this);
    return this;
  }

  public CommunityInvitationFlow community(Consumer<CommunityInvitationFlow> step) {
    step.accept(this);
    return this;
  }

  public CommunityInvitationFlow interaction(Consumer<CommunityInvitationFlow> step) {
    step.accept(this);
    return this;
  }

  public CommunityInvitationFlow save(Consumer<CommunityInvitationFlow> step) {
    step.accept(this);
    return this;
  }

  public CommunityInvitationFlow notify(Consumer<CommunityInvitationFlow> step) {
    step.accept(this);
    return this;
  }

  public CommunityInvitationFlow loadCommunity(CommunityRepository communityRepository) {
    Community community = communityRepository.findById(CommunityId.of(context.communityId()))
        .orElseThrow(() -> new IllegalArgumentException("커뮤니티를 찾을 수 없습니다."));
    context.community(community);
    return this;
  }

  public CommunityInvitationFlow loadInitiator(UserContextManager userContextManager) {
    User initiator = userContextManager
        .findByEmail(context.userDetails().getUsername(), User.class)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    context.initiator(initiator);
    return this;
  }

  public CommunityInvitationFlow loadInvitees(UserContextManager userContextManager) {
    for (String email : context.emails()) {
      if (email == null || email.isBlank()) {
        continue;
      }
      userContextManager.findByEmail(email, User.class)
          .ifPresent(context::addInvitee);
    }
    return this;
  }

  public CommunityInvitationFlow invite(MemberManager memberManager) {
    Community community = context.community();
    User initiator = context.initiator();
    for (User invitee : context.invitees()) {
      if (invitee == null || invitee.getId() == null) {
        continue;
      }
      if (invitee.getId().equals(initiator.getId())) {
        continue;
      }
      Interaction interaction = memberManager.invite(community, initiator, invitee);
      if (interaction.isFailed()) {
        continue;
      }
      context.addSuccessfulInvitee(invitee);
    }
    return this;
  }

  public CommunityInvitationFlow sendInvites(AlarmContextManager alarmContextManager) {
    Community community = context.community();
    User initiator = context.initiator();
    if (community == null || initiator == null) {
      return this;
    }
    String initiatorName = resolveInitiatorName(initiator);
    for (User invitee : context.successfulInvitees()) {
      if (invitee == null || invitee.getId() == null) {
        continue;
      }
      alarmContextManager.send(
          AlarmFactory.createGroupInvitationAlarm(
              invitee.getId().toString(),
              community.getId().toString(),
              community.getName(),
              new Trigger(initiatorName, "USER")
          )
      );
    }
    return this;
  }

  private String resolveInitiatorName(User initiator) {
    UserAccountView view = UserAccountView.from(initiator);
    String name = view.displayName();
    if (name != null && !name.isBlank()) {
      return name;
    }
    String fallback = view.email();
    return fallback == null || fallback.isBlank() ? "SYSTEM" : fallback;
  }

  public CommunityInvitationFlow persist(CommunityRepository communityRepository) {
    communityRepository.save(context.community());
    return this;
  }

  public CommunityInvitation end() {
    return context;
  }
}
