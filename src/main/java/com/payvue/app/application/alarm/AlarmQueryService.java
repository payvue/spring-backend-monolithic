package com.payvue.app.application.alarm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payvue.app.domain.alarm.ChannelCategory;
import com.payvue.app.domain.alarm.GroupAlarmContent;
import com.payvue.app.domain.alarm.SystemAlarmContent;
import com.payvue.app.domain.alarm.TransactionAlarmContent;
import com.payvue.app.infrastructure.persistence.alarm.AlarmEntity;
import com.payvue.app.infrastructure.persistence.alarm.AlarmJpaRepository;
import com.payvue.app.infrastructure.persistence.alarm.AlarmReadEntity;
import com.payvue.app.infrastructure.persistence.alarm.AlarmReadJpaRepository;
import com.payvue.app.presentation.rest.alarm.AlarmEvent;
import com.payvue.app.presentation.rest.alarm.AlarmContent;
import com.payvue.app.presentation.rest.alarm.GroupContent;
import com.payvue.app.presentation.rest.alarm.AlarmMeta;
import com.payvue.app.presentation.rest.alarm.AlarmNotification;
import com.payvue.app.presentation.rest.alarm.AlarmNotificationList;
import com.payvue.app.presentation.rest.alarm.SystemContent;
import com.payvue.app.presentation.rest.alarm.TransactionContent;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.generic.alarm.AlarmOperationUnit;
import com.payvue.generic.alarm.RecipientType;
import com.payvue.generic.alarm.Trigger;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlarmQueryService {

  private static final int DEFAULT_PAGE_SIZE = 20;
  private static final String DEFAULT_CURRENCY = "CASH";
  private static final List<String> RECEIVED_ALARM_TYPES =
      List.of(ChannelCategory.IN_APP.name());

  private final AlarmJpaRepository alarmJpaRepository;
  private final AlarmReadJpaRepository alarmReadJpaRepository;
  private final UserContextManager userContextManager;
  private final CommunityContextManager communityContextManager;
  private final ObjectMapper objectMapper;

  public AlarmNotificationList fetchReceived(String email, int page) {
    User user = userContextManager.findByEmail(email, User.class)
        .orElseThrow(() -> new IllegalArgumentException("등록된 사용자를 찾을 수 없습니다."));
    List<String> recipientValues = buildRecipientValues(user, email);
    List<String> communityIds = resolveCommunityIds(user);
    Pageable pageable = PageRequest.of(
        Math.max(page - 1, 0),
        DEFAULT_PAGE_SIZE
    );
    List<AlarmEntity> alarms = fetchOrderedAlarms(
        recipientValues,
        communityIds,
        email,
        pageable
    );
    Set<String> readIds = loadReadIds(alarms, email);
    List<AlarmNotification> notifications = alarms.stream()
        .map(entity -> toNotification(entity, readIds.contains(entity.getAlarmId())))
        .toList();
    return new AlarmNotificationList(notifications);
  }

  private List<AlarmEntity> fetchOrderedAlarms(
      List<String> recipientValues,
      List<String> communityIds,
      String reader,
      Pageable pageable
  ) {
    if (communityIds.isEmpty()) {
      return alarmJpaRepository.findReceivedOrderedForUser(
              RecipientType.USER.name(),
              recipientValues,
              RECEIVED_ALARM_TYPES,
              reader,
              pageable
          )
          .getContent();
    }
    return alarmJpaRepository.findReceivedOrderedForUserAndCommunities(
            RecipientType.USER.name(),
            recipientValues,
            RecipientType.COMMUNITY.name(),
            communityIds,
            RECEIVED_ALARM_TYPES,
            reader,
            pageable
        )
        .getContent();
  }

  private List<String> buildRecipientValues(User user, String email) {
    java.util.LinkedHashSet<String> values = new java.util.LinkedHashSet<>();
    if (user != null && user.getId() != null) {
      values.add(user.getId().toString());
    }
    if (email != null && !email.isBlank()) {
      values.add(email);
    }
    return java.util.List.copyOf(values);
  }

  private List<String> resolveCommunityIds(User user) {
    if (user == null) {
      return List.of();
    }
    Set<Community> communities = communityContextManager.resolve(user);
    if (communities == null || communities.isEmpty()) {
      return List.of();
    }
    return communities.stream()
        .map(Community::getId)
        .filter(Objects::nonNull)
        .map(Object::toString)
        .toList();
  }

  private Set<String> loadReadIds(List<AlarmEntity> alarms, String reader) {
    if (alarms == null || alarms.isEmpty()) {
      return Set.of();
    }
    List<String> alarmIds = alarms.stream()
        .map(AlarmEntity::getAlarmId)
        .filter(Objects::nonNull)
        .toList();
    return alarmReadJpaRepository.findByAlarmIdInAndReader(alarmIds, reader)
        .stream()
        .map(AlarmReadEntity::getAlarmId)
        .collect(java.util.stream.Collectors.toSet());
  }

  private AlarmNotification toNotification(AlarmEntity entity, boolean isRead) {
    return new AlarmNotification(
        entity.getAlarmId(),
        entity.getTitle(),
        entity.getMessage(),
        entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString(),
        toEvent(entity, isRead)
    );
  }

  private AlarmEvent toEvent(AlarmEntity entity, boolean isRead) {
    AlarmOperationUnit unit = resolveOperationUnit(entity.getContentJson());
    String type = resolveEventType(unit);
    AlarmMeta meta = new AlarmMeta(isRead, entity.getMessage());
    AlarmContent content = resolveContent(entity.getContentJson(), unit);
    return new AlarmEvent(type, meta, content);
  }

  private AlarmOperationUnit resolveOperationUnit(String contentJson) {
    if (contentJson == null || contentJson.isBlank()) {
      return null;
    }
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

  private String resolveEventType(AlarmOperationUnit unit) {
    if (unit == null) {
      return "System";
    }
    return switch (unit.category()) {
      case TRANSACTION -> "Transaction";
      case GROUP -> "Group";
      case SYSTEM -> "System";
    };
  }

  private AlarmContent resolveContent(String contentJson, AlarmOperationUnit unit) {
    if (contentJson == null || contentJson.isBlank() || unit == null) {
      return null;
    }
    try {
      return switch (unit.category()) {
        case TRANSACTION -> toContent(objectMapper.readValue(contentJson, TransactionAlarmContent.class));
        case GROUP -> toContent(objectMapper.readValue(contentJson, GroupAlarmContent.class));
        case SYSTEM -> toContent(objectMapper.readValue(contentJson, SystemAlarmContent.class));
      };
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Alarm content 파싱에 실패했습니다.", e);
    }
  }

  private TransactionContent toContent(TransactionAlarmContent content) {
    if (content == null) {
      return null;
    }
    String sender = firstNonBlank(
        content.getInitiatedBy(),
        content.getApprovedBy(),
        resolveTriggerName(content.getTrigger())
    );
    return new TransactionContent(
        content.getAmount(),
        DEFAULT_CURRENCY,
        sender,
        content.getRequestedBy()
    );
  }

  private GroupContent toContent(GroupAlarmContent content) {
    if (content == null) {
      return null;
    }
    return new GroupContent(
        content.getGroupId(),
        content.getGroupName(),
        content.getTrigger() == null ? null : content.getTrigger().name(),
        content.isRequiresAction()
    );
  }

  private SystemContent toContent(SystemAlarmContent content) {
    if (content == null) {
      return null;
    }
    return new SystemContent(
        content.getDevice(),
        content.getLocation(),
        content.getIpAddress(),
        content.getReason()
    );
  }

  private String resolveTriggerName(Trigger trigger) {
    return trigger == null ? null : trigger.name();
  }

  private String firstNonBlank(String first, String second, String third) {
    if (first != null && !first.isBlank()) {
      return first;
    }
    if (second != null && !second.isBlank()) {
      return second;
    }
    if (third != null && !third.isBlank()) {
      return third;
    }
    return null;
  }
}
