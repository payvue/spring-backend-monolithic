package com.payvue.app.infrastructure.persistence.alarm;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payvue.app.domain.alarm.EmailAlarm;
import com.payvue.app.domain.alarm.GroupAlarmContent;
import com.payvue.app.domain.alarm.InAppAlarm;
import com.payvue.app.domain.alarm.SystemAlarmContent;
import com.payvue.app.domain.alarm.TransactionAlarmContent;
import com.payvue.app.domain.alarm.verificationEmailContent;
import com.payvue.generic.alarm.Alarm;
import com.payvue.generic.alarm.AlarmFactory;
import com.payvue.generic.alarm.AlarmOperationUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AlarmEntityTest {

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Test
  @DisplayName("Email 알람을 엔티티로 변환한다")
  void mapsEmailAlarmToEntity() {
    Alarm alarm = AlarmFactory.createVerificationEmail(
        "user@payvue.com",
        "PayVue",
        "verify",
        "https://payvue.com/verify"
    );

    AlarmEntity entity = AlarmEntity.toEntity(alarm, objectMapper);

    assertThat(entity.getAlarmType()).isEqualTo("EMAIL");
    assertThat(entity.getRecipientType()).isEqualTo("USER");
    assertThat(entity.getRecipientValue()).isEqualTo("user@payvue.com");
    assertThat(entity.getContentJson()).contains("verifyUrl");
    assertThat(entity.getTemplateVariablesJson()).contains("userName");
  }

  @Test
  @DisplayName("Email 알람 엔티티를 도메인으로 복원한다")
  void mapsEmailEntityToDomain() {
    Alarm source = AlarmFactory.createVerificationEmail(
        "user@payvue.com",
        "PayVue",
        "verify",
        "https://payvue.com/verify"
    );
    AlarmEntity entity = AlarmEntity.toEntity(source, objectMapper);

    Alarm restored = AlarmEntity.toDomain(entity, objectMapper);

    assertThat(restored).isInstanceOf(EmailAlarm.class);
    assertThat(restored.getId()).isEqualTo(source.getId());
    assertThat(restored.getRecipient()).isEqualTo(source.getRecipient());
    assertThat(restored.getTitle()).isEqualTo(source.getTitle());
    assertThat(restored.getMessage()).isEqualTo(source.getMessage());
    assertThat(restored.getDescription()).isEqualTo(source.getMessage());
    assertThat(restored.getContent()).isInstanceOf(verificationEmailContent.class);
  }

  @Test
  @DisplayName("Group 알람 엔티티를 도메인으로 복원한다")
  void mapsGroupAlarmToDomain() {
    Alarm source = AlarmFactory.createGroupInvitationAlarm(
        "user-id",
        "group-1",
        "PayVue",
        new com.payvue.generic.alarm.Trigger("Alice", "USER")
    );
    AlarmEntity entity = AlarmEntity.toEntity(source, objectMapper);

    Alarm restored = AlarmEntity.toDomain(entity, objectMapper);

    assertThat(restored).isInstanceOf(InAppAlarm.class);
    assertThat(restored.getContent()).isInstanceOf(GroupAlarmContent.class);
    GroupAlarmContent content = (GroupAlarmContent) restored.getContent();
    assertThat(content.getGroupId()).isEqualTo("group-1");
    assertThat(content.getGroupName()).isEqualTo("PayVue");
    assertThat(content.isRequiresAction()).isTrue();
    assertThat(content.getOperationUnit()).isEqualTo(AlarmOperationUnit.INVITE);
  }

  @Test
  @DisplayName("Transaction 알람 엔티티를 도메인으로 복원한다")
  void mapsTransactionAlarmToDomain() {
    Alarm source = AlarmFactory.createPaymentApprovedAlarm(
        "user-id",
        "tx-1",
        1500L,
        "Approver"
    );
    AlarmEntity entity = AlarmEntity.toEntity(source, objectMapper);

    Alarm restored = AlarmEntity.toDomain(entity, objectMapper);

    assertThat(restored).isInstanceOf(InAppAlarm.class);
    assertThat(restored.getContent()).isInstanceOf(TransactionAlarmContent.class);
    TransactionAlarmContent content = (TransactionAlarmContent) restored.getContent();
    assertThat(content.getTransactionId()).isEqualTo("tx-1");
    assertThat(content.getAmount()).isEqualTo(1500L);
    assertThat(content.getApprovedBy()).isEqualTo("Approver");
    assertThat(content.getOperationUnit()).isEqualTo(AlarmOperationUnit.PAYMENT);
  }

  @Test
  @DisplayName("System 알람 엔티티를 도메인으로 복원한다")
  void mapsSystemAlarmToDomain() {
    Alarm source = AlarmFactory.createSecurityAlarm(
        "user-id",
        "iPhone",
        "Seoul",
        "127.0.0.1"
    );
    AlarmEntity entity = AlarmEntity.toEntity(source, objectMapper);

    Alarm restored = AlarmEntity.toDomain(entity, objectMapper);

    assertThat(restored).isInstanceOf(InAppAlarm.class);
    assertThat(restored.getContent()).isInstanceOf(SystemAlarmContent.class);
    SystemAlarmContent content = (SystemAlarmContent) restored.getContent();
    assertThat(content.getDevice()).isEqualTo("iPhone");
    assertThat(content.getLocation()).isEqualTo("Seoul");
    assertThat(content.getIpAddress()).isEqualTo("127.0.0.1");
    assertThat(content.getOperationUnit()).isEqualTo(AlarmOperationUnit.SECURITY);
  }
}
