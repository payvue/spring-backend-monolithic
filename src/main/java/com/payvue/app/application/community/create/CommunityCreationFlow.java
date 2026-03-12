package com.payvue.app.application.community.create;

import com.payvue.app.domain.community.RecipientType;
import com.payvue.app.domain.account.CommandType;
import com.payvue.app.domain.community.Personal;
import com.payvue.app.domain.community.RoleCatalog;
import com.payvue.app.domain.community.CommunityCategory;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.core.community.CommunityInitCommand;
import com.payvue.core.community.CommunityRepository;
import com.payvue.core.community.Participant;
import com.payvue.core.community.RoleDescriptor;
import com.payvue.shared.Money;
import com.payvue.supporting.interaction.Interaction;
import com.payvue.supporting.interaction.InteractionExecutor;
import com.payvue.supporting.interaction.InviteDecision;
import com.payvue.supporting.interaction.ManageIntent;
import com.payvue.app.application.transaction.TransactionUseCase;
import com.payvue.app.application.transaction.command.TransferCashCommand;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import com.payvue.generic.alarm.AlarmContextManager;
import com.payvue.generic.alarm.AlarmFactory;
import com.payvue.generic.alarm.Trigger;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.security.core.userdetails.UserDetails;

public final class CommunityCreationFlow {
  private final CommunityCreation context;

  private CommunityCreationFlow(CommunityCreation context) {
    this.context = context;
  }

  public static CommunityCreationFlow start(UserDetails userDetails, boolean standAlone, CommunityCreateCommand command) {
    return new CommunityCreationFlow(new CommunityCreation(userDetails, standAlone, command));
  }

  public CommunityCreationFlow user(Consumer<CommunityCreationFlow> step) {
    step.accept(this);
    return this;
  }

  public CommunityCreationFlow community(Consumer<CommunityCreationFlow> step) {
    step.accept(this);
    return this;
  }

  public CommunityCreationFlow asset(Consumer<CommunityCreationFlow> step) {
    step.accept(this);
    return this;
  }

  public CommunityCreationFlow interaction(Consumer<CommunityCreationFlow> step) {
    step.accept(this);
    return this;
  }

  public CommunityCreationFlow transaction(Consumer<CommunityCreationFlow> step) {
    step.accept(this);
    return this;
  }

  public CommunityCreationFlow notify(Consumer<CommunityCreationFlow> step) {
    step.accept(this);
    return this;
  }

  public CommunityCreationFlow save(Consumer<CommunityCreationFlow> step) {
    step.accept(this);
    return this;
  }

  public CommunityCreationFlow validate(CommunityRepository communityRepository) {
    if (context.standAlone()) {
      throw new IllegalArgumentException("개인 커뮤니티는 등록할 수 없습니다.");
    }
    if (communityRepository.existsByName(context.command().name())) {
      throw new IllegalArgumentException("이미 등록된 커뮤니티입니다.");
    }

    return this;
  }

  public CommunityCreationFlow loadUser(UserContextManager userContextManager) {
    User user = userContextManager.findByEmail(context.userDetails().getUsername(), User.class)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    context.user(user);
    return this;
  }

  public CommunityCreationFlow createCommunity(
      CommunityContextManager communityContextManager
  ) {

    Personal personal = communityContextManager.resolve(context.user(), CommunityCategory.PERSONAL.name())
            .stream()
            .filter(Personal.class::isInstance)
            .map(Personal.class::cast)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("비정상 접근입니다."));

    UserAccountView view = UserAccountView.from(context.user());
    Community created = communityContextManager.init(
        new CommunityInitCommand(CommunityCategory.COOPERATIVE.name(), view)
    );
    created.rename(context.command().name());
    created.changeDescription(context.command().description());

    context.personalCommunity(personal);
    context.community(communityContextManager.save(created));
    return this;
  }

  public CommunityCreationFlow loadInvitees(UserContextManager userContextManager) {
    List<String> emails = context.command().invitedMembers();
    if (emails == null) {
      return this;
    }
    for (String email : emails) {
      userContextManager.findByEmail(email, User.class).ifPresent(context::addInvitee);
    }
    return this;
  }

  public CommunityCreationFlow inviteMembers(InteractionExecutor interactionExecutor, RoleCatalog roleCatalog) {
    User owner = context.user();
    Community community = context.community();
    for (User invitee : context.invited()) {
      if (Objects.equals(invitee.getId(), owner.getId())) {
        continue;
      }
      Interaction interaction = interactionExecutor.executeInternal(
          new InviteDecision(
              new ManageIntent(
                  to(owner, roleCatalog.admin()),
                  to(invitee, roleCatalog.member())
              )
          ),
          community
      );
      if (interaction.isFailed()) {
        continue;
      }
      context.addSuccessfulInvitee(invitee);
    }
    return this;
  }

  public CommunityCreationFlow sendInvites(AlarmContextManager alarmContextManager) {
    Community community = context.community();
    User owner = context.user();
    if (community == null || owner == null) {
      return this;
    }
    String initiatorName = resolveInitiatorName(owner);
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

  public CommunityCreationFlow registerOwnerMembership(UserContextManager userContextManager) {
    User owner = context.user();
    owner.joinCommunity(context.community().getId().toString());
    userContextManager.save(owner, CommandType.UPDATE_PROFILE::name);
    return this;
  }

  public CommunityCreationFlow transferCash(TransactionUseCase transactionUseCase) {

    if (context.command().initialAmount() == null || Money.won(context.command().initialAmount()).isZero()) {
      return this;
    }
    TransferCashCommand command = new TransferCashCommand(
        context.community().getId().toString(),
        RecipientType.COMMUNITY,
        context.command().initialAmount(),
        "KRW",
        "커뮤니티 생성 초기 자산 이체",
        null
    );
    transactionUseCase.transferCash(context.userDetails(), command, null);
    return this;
  }

  public CommunityCreationFlow persist(CommunityRepository communityRepository) {
    communityRepository.save(context.community());
    return this;
  }

  public CommunityCreation end() {
    return context;
  }

  private Participant to(User user, RoleDescriptor role) {
    if (user == null || user.getId() == null) {
      throw new IllegalArgumentException("사용자 정보가 없습니다.");
    }
    return Participant.of(user.getId().getValue(), role);
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
}
