package com.payvue.app.application.alarm.invite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payvue.app.domain.alarm.GroupAlarmContent;
import com.payvue.app.domain.community.Cooperative;
import com.payvue.app.domain.alarm.InAppAlarm;
import com.payvue.app.domain.account.CommandType;
import com.payvue.app.infrastructure.persistence.alarm.AlarmEntity;
import com.payvue.app.infrastructure.persistence.alarm.AlarmJpaRepository;
import com.payvue.core.community.Community;
import com.payvue.app.domain.community.CommunityCategory;
import com.payvue.core.community.CategoryQuery;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.core.community.CommunityId;
import com.payvue.core.community.policy.CommunityPolicy;
import com.payvue.generic.alarm.AlarmContextManager;
import com.payvue.generic.alarm.AlarmFactory;
import com.payvue.generic.alarm.AlarmOperationUnit;
import com.payvue.generic.alarm.Trigger;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public final class AlarmInvitationHandleFlow {
  private final AlarmInvitationHandle context;

  private AlarmInvitationHandleFlow(AlarmInvitationHandle context) {
    this.context = context;
  }

  public static AlarmInvitationHandleFlow start(String alarmId, String action, String readerEmail) {
    return new AlarmInvitationHandleFlow(new AlarmInvitationHandle(alarmId, action, readerEmail));
  }

  public AlarmInvitationHandleFlow alarm(Consumer<AlarmInvitationHandleFlow> step) {
    step.accept(this);
    return this;
  }

  public AlarmInvitationHandleFlow user(Consumer<AlarmInvitationHandleFlow> step) {
    step.accept(this);
    return this;
  }

  public AlarmInvitationHandleFlow community(Consumer<AlarmInvitationHandleFlow> step) {
    step.accept(this);
    return this;
  }

  public AlarmInvitationHandleFlow action(Consumer<AlarmInvitationHandleFlow> step) {
    step.accept(this);
    return this;
  }

  public AlarmInvitationHandleFlow notify(Consumer<AlarmInvitationHandleFlow> step) {
    step.accept(this);
    return this;
  }

  public AlarmInvitationHandleFlow loadAlarm(
      AlarmJpaRepository alarmJpaRepository,
      ObjectMapper objectMapper
  ) {
    AlarmEntity entity = alarmJpaRepository.findByAlarmId(context.alarmId())
        .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));
    context.alarm(entity);
    context.content(resolveInviteContent(entity, objectMapper));
    return this;
  }

  public AlarmInvitationHandleFlow loadUser(UserContextManager userContextManager) {
    User user = userContextManager.findByEmail(context.readerEmail(), User.class)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    ensureRecipient(context.alarm(), user, context.readerEmail());
    context.user(user);
    return this;
  }

  public AlarmInvitationHandleFlow loadCommunity(CommunityContextManager communityContextManager) {
    GroupAlarmContent content = context.content();
    Community community = communityContextManager.get(
        new CategoryQuery(
            CommunityCategory.COOPERATIVE.name(),
            CommunityId.of(UUID.fromString(content.getGroupId()))
        )
    );
    if (!(community instanceof Cooperative)) {
      throw new IllegalArgumentException("초대 응답은 협동 커뮤니티만 지원합니다.");
    }
    context.community(community);
    return this;
  }

  public AlarmInvitationHandleFlow applyAction(
      CommunityContextManager communityContextManager,
      UserContextManager userContextManager,
      com.payvue.app.application.alarm.AlarmReadService alarmReadService
  ) {
    AlarmAction decision = AlarmAction.from(context.action());
    Community community = context.community();
    Cooperative cooperative = (Cooperative) community;
    User user = context.user();
    UUID userId = user.getId().getValue();
    if (!community.isParticipant(userId)) {
      throw new IllegalArgumentException("초대 대상이 아닙니다.");
    }
    CommunityPolicy policy = c -> true;
    if (decision == AlarmAction.ACCEPT) {
      user.joinCommunity(cooperative.getId().toString());
      cooperative.activateMember(userId, policy);
      userContextManager.save(user, CommandType.UPDATE_PROFILE::name);
    } else {
      cooperative.removeMember(userId, policy);
    }
    communityContextManager.save(community);
    alarmReadService.markAsRead(context.alarmId(), context.readerEmail());
    return this;
  }

  public AlarmInvitationHandleFlow sendAlarm(AlarmContextManager alarmContextManager) {
    AlarmAction decision = AlarmAction.from(context.action());

    if (decision == AlarmAction.ACCEPT) {
      Community community = context.community();
      User user = context.user();
      UserAccountView joiner = UserAccountView.from(user);
      Trigger trigger = new Trigger(joiner.displayName(), "MEMBER");
      for (UUID memberId : community.getParticipantIds()) {
        if (memberId.equals(user.getId().getValue())) {
          continue;
        }
        InAppAlarm alarm = AlarmFactory.createCommunityJoinAlarm(
                memberId.toString(),
                community.getId().getValue().toString(),
                community.getName(),
                trigger
        );
        alarmContextManager.send(alarm);
      }
    }

    if(decision == AlarmAction.REJECT) {
      Community community = context.community();
      User user = context.user();
      UserAccountView joiner = UserAccountView.from(user);
      Trigger trigger = new Trigger(joiner.displayName(), "MEMBER");
      for (UUID memberId : community.getParticipantIds()) {
        if (memberId.equals(user.getId().getValue())) {
          continue;
        }
        InAppAlarm alarm = AlarmFactory.createCommunityInviteRejectedAlarm(
                memberId.toString(),
                community.getId().getValue().toString(),
                community.getName(),
                trigger
        );
        alarmContextManager.send(alarm);
      }
    }

    return this;
  }

  public void end() {
  }

  private GroupAlarmContent resolveInviteContent(AlarmEntity entity, ObjectMapper objectMapper) {
    String contentJson = entity.getContentJson();
    if (contentJson == null || contentJson.isBlank()) {
      throw new IllegalArgumentException("알림 내용이 비어 있습니다.");
    }
    AlarmOperationUnit unit = resolveOperationUnit(contentJson, objectMapper);
    if (unit != AlarmOperationUnit.INVITE) {
      throw new IllegalArgumentException("초대 알림이 아닙니다.");
    }
    GroupAlarmContent content = readContent(contentJson, objectMapper);
    if (!content.isRequiresAction()) {
      throw new IllegalArgumentException("처리할 수 없는 알림입니다.");
    }
    return content;
  }

  private AlarmOperationUnit resolveOperationUnit(String contentJson, ObjectMapper objectMapper) {
    try {
      Map<String, Object> payload = objectMapper.readValue(contentJson, Map.class);
      String value = Objects.toString(payload.get("operationUnit"), "");
      if (value.isBlank()) {
        return null;
      }
      return AlarmOperationUnit.valueOf(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Alarm content 파싱에 실패했습니다.", e);
    }
  }

  private GroupAlarmContent readContent(String contentJson, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(contentJson, GroupAlarmContent.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Alarm content 파싱에 실패했습니다.", e);
    }
  }

  private void ensureRecipient(AlarmEntity entity, User user, String email) {
    String recipientValue = entity.getRecipientValue();
    if (recipientValue == null) {
      throw new IllegalArgumentException("알림 수신자를 확인할 수 없습니다.");
    }
    String userId = user.getId() == null ? null : user.getId().toString();
    if (!recipientValue.equals(email) && !recipientValue.equals(userId)) {
      throw new IllegalArgumentException("알림 수신자가 아닙니다.");
    }
  }

  private enum AlarmAction {
    ACCEPT,
    REJECT;

    static AlarmAction from(String value) {
      if (value == null || value.isBlank()) {
        throw new IllegalArgumentException("action은 필수입니다.");
      }
      return switch (value.toLowerCase()) {
        case "accept" -> ACCEPT;
        case "reject" -> REJECT;
        default -> throw new IllegalArgumentException("지원하지 않는 action입니다.");
      };
    }
  }
}
