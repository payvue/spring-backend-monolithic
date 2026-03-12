package com.payvue.app.infrastructure.persistence.alarm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payvue.app.domain.alarm.ChannelCategory;
import com.payvue.app.domain.alarm.EmailAlarm;
import com.payvue.app.domain.alarm.GroupAlarmContent;
import com.payvue.app.domain.alarm.InAppAlarm;
import com.payvue.app.domain.alarm.SystemAlarmContent;
import com.payvue.app.domain.alarm.TransactionAlarmContent;
import com.payvue.app.domain.alarm.verificationEmailContent;
import com.payvue.generic.alarm.Alarm;
import com.payvue.generic.alarm.AlarmContent;
import com.payvue.generic.alarm.AlarmId;
import com.payvue.generic.alarm.AlarmOperationUnit;
import com.payvue.generic.alarm.Recipient;
import com.payvue.generic.alarm.RecipientType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(
    name = "alarms",
    indexes = {
        @Index(name = "idx_alarm_id", columnList = "alarmId"),
        @Index(name = "idx_recipient", columnList = "recipientValue"),
        @Index(name = "idx_status", columnList = "status")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlarmEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 36)
    private String alarmId;
    
    @Column(nullable = false, length = 20)
    private String alarmType;  // EMAIL, IN_APP
    
    @Column(nullable = false, length = 20)
    private String recipientType;  // USER, COMMUNITY, BROADCAST
    
    @Column(nullable = false, length = 255)
    private String recipientValue;
    
    @Column(nullable = false, length = 255)
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column(nullable = false, length = 20)
    private String status;  // CREATED, SENT, FAILED
    
    @Column(length = 500)
    private String failureReason;
    
    @Column(columnDefinition = "TEXT")
    private String contentJson;  // AlarmContent를 JSON으로 직렬화
    
    @Column(columnDefinition = "TEXT")
    private String templateVariablesJson;  // EmailAlarm의 템플릿 변수

    public AlarmEntity(
            String alarmId,
            String alarmType,
            String recipientType,
            String recipientValue,
            String title,
            String message,
            String status,
            String contentJson,
            String templateVariablesJson
    ) {
        this.alarmId = alarmId;
        this.alarmType = alarmType;
        this.recipientType = recipientType;
        this.recipientValue = recipientValue;
        this.title = title;
        this.message = message;
        this.createdAt = Instant.now();
        this.status = status;
        this.contentJson = contentJson;
        this.templateVariablesJson = templateVariablesJson;
    }

    void applyId(Long id) {
        this.id = id;
    }

    static AlarmEntity toEntity(Alarm alarm, ObjectMapper objectMapper) {
        Objects.requireNonNull(alarm, "alarm is required");
        Objects.requireNonNull(objectMapper, "objectMapper is required");

        String contentJson = writeJson(objectMapper, alarm.getContent());
        String templateVariablesJson = resolveTemplateVariablesJson(alarm, objectMapper);

        return new AlarmEntity(
                alarm.getId().value().toString(),
                resolveAlarmType(alarm),
                alarm.getRecipient().getType().name(),
                alarm.getRecipient().getValue(),
                alarm.getTitle(),
                alarm.getMessage(),
                "CREATED",
                contentJson,
                templateVariablesJson
        );
    }

    static Alarm toDomain(AlarmEntity entity, ObjectMapper objectMapper) {
        Objects.requireNonNull(entity, "entity is required");
        Objects.requireNonNull(objectMapper, "objectMapper is required");

        AlarmContent content = resolveContent(entity, objectMapper);
        Recipient recipient = resolveRecipient(entity.getRecipientType(), entity.getRecipientValue());
        AlarmId id = new AlarmId(java.util.UUID.fromString(entity.getAlarmId()));
        String description = entity.getMessage();

        if (ChannelCategory.EMAIL.name().equals(entity.getAlarmType())) {
            return new EmailAlarm(id, recipient, entity.getTitle(), entity.getMessage(), description, content);
        }
        if (ChannelCategory.IN_APP.name().equals(entity.getAlarmType())) {
            return new InAppAlarm(id, recipient, entity.getTitle(), entity.getMessage(), description, content);
        }

        throw new IllegalArgumentException("지원하지 않는 알림 타입입니다: " + entity.getAlarmType());
    }

    private static AlarmContent resolveContent(AlarmEntity entity, ObjectMapper objectMapper) {
        String contentJson = Objects.requireNonNull(entity.getContentJson(), "contentJson is required");
        if (ChannelCategory.EMAIL.name().equals(entity.getAlarmType())) {
            return readJson(objectMapper, contentJson, verificationEmailContent.class);
        }

        Map<String, Object> payload = readJson(objectMapper, contentJson, Map.class);
        String operationUnitValue = Objects.toString(payload.get("operationUnit"), "");
        AlarmOperationUnit operationUnit = AlarmOperationUnit.valueOf(operationUnitValue);

        return switch (operationUnit.category()) {
            case GROUP -> readJson(objectMapper, contentJson, GroupAlarmContent.class);
            case TRANSACTION -> readJson(objectMapper, contentJson, TransactionAlarmContent.class);
            case SYSTEM -> readJson(objectMapper, contentJson, SystemAlarmContent.class);
        };
    }

    private static Recipient resolveRecipient(String type, String value) {
        RecipientType recipientType = RecipientType.valueOf(type);
        if (recipientType == RecipientType.USER) {
            return Recipient.ofUser(value);
        }
        if (recipientType == RecipientType.COMMUNITY) {
            return Recipient.ofCommunity(value);
        }
        throw new IllegalArgumentException("Unsupported recipient type: " + type);
    }

    private static String resolveAlarmType(Alarm alarm) {
        if (alarm instanceof EmailAlarm) {
            return ChannelCategory.EMAIL.name();
        }
        if (alarm instanceof InAppAlarm) {
            return ChannelCategory.IN_APP.name();
        }
        throw new IllegalArgumentException("지원하지 않는 알림 타입입니다: " + alarm.getClass().getSimpleName());
    }

    private static String resolveTemplateVariablesJson(Alarm alarm, ObjectMapper objectMapper) {
        if (alarm instanceof EmailAlarm emailAlarm) {
            AlarmContent content = emailAlarm.getContent();
            if (content instanceof verificationEmailContent verificationContent) {
                return writeJson(objectMapper, verificationContent.toVariables());
            }
        }
        return "";
    }

    private static String writeJson(ObjectMapper objectMapper, Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Alarm JSON 변환에 실패했습니다.", e);
        }
    }

    private static <T> T readJson(ObjectMapper objectMapper, String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Alarm JSON 파싱에 실패했습니다.", e);
        }
    }
}
