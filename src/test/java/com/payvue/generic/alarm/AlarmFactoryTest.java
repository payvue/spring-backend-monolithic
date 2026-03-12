package com.payvue.generic.alarm;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.app.domain.alarm.EmailAlarm;
import com.payvue.app.domain.alarm.GroupAlarmContent;
import com.payvue.app.domain.alarm.InAppAlarm;
import com.payvue.app.domain.alarm.SystemAlarmContent;
import com.payvue.app.domain.alarm.TransactionAlarmContent;
import com.payvue.app.domain.alarm.verificationEmailContent;
import com.payvue.app.infrastructure.mail.EmailTemplateId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AlarmFactoryTest {

    @Test
    @DisplayName("알림 팩토리는 그룹 결제 알림을 생성한다")
    void createGroupPaymentAlarm() {
        Trigger trigger = new Trigger("Alice", "USER");

        InAppAlarm alarm = AlarmFactory.createGroupPaymentAlarm(
                "user-1",
                "group-1",
                "PayVue",
                trigger,
                1000L,
                "merchant"
        );

        assertThat(alarm.getRecipient().getValue()).isEqualTo("user-1");
        assertThat(alarm.channelKey()).isEqualTo("IN_APP");
        GroupAlarmContent content = (GroupAlarmContent) alarm.getContent();
        assertThat(content.getOperationUnit()).isEqualTo(AlarmOperationUnit.BALANCE_CHARGE);
        assertThat(content.getGroupId()).isEqualTo("group-1");
        assertThat(content.getGroupName()).isEqualTo("PayVue");
        assertThat(content.getTrigger()).isEqualTo(trigger);
        assertThat(content.isRequiresAction()).isFalse();
    }

    @Test
    @DisplayName("알림 팩토리는 그룹 결제 요청 알림을 생성한다")
    void createGroupPaymentRequestAlarm() {
        Trigger trigger = new Trigger("Alice", "USER");

        InAppAlarm alarm = AlarmFactory.createGroupPaymentRequestAlarm(
                "user-1",
                "group-1",
                "PayVue",
                trigger,
                1000L,
                "merchant"
        );

        GroupAlarmContent content = (GroupAlarmContent) alarm.getContent();
        assertThat(content.getOperationUnit()).isEqualTo(AlarmOperationUnit.PAYMENT_REQUEST);
        assertThat(content.getGroupId()).isEqualTo("group-1");
        assertThat(content.getGroupName()).isEqualTo("PayVue");
        assertThat(content.getTrigger()).isEqualTo(trigger);
        assertThat(content.isRequiresAction()).isFalse();
    }

    @Test
    @DisplayName("알림 팩토리는 그룹 송금 알림을 생성한다")
    void createGroupTransferAlarm() {
        Trigger trigger = new Trigger("Alice", "USER");

        InAppAlarm alarm = AlarmFactory.createGroupTransferAlarm(
                "user-1",
                "group-1",
                "PayVue",
                trigger,
                "Bob",
                500L
        );

        GroupAlarmContent content = (GroupAlarmContent) alarm.getContent();
        assertThat(content.getOperationUnit()).isEqualTo(AlarmOperationUnit.TRANSFER);
        assertThat(content.getGroupId()).isEqualTo("group-1");
        assertThat(content.getGroupName()).isEqualTo("PayVue");
        assertThat(content.getTrigger()).isEqualTo(trigger);
        assertThat(content.isRequiresAction()).isFalse();
    }

    @Test
    @DisplayName("알림 팩토리는 그룹 초대 알림을 생성한다")
    void createGroupInvitationAlarm() {
        Trigger trigger = new Trigger("Alice", "USER");

        InAppAlarm alarm = AlarmFactory.createGroupInvitationAlarm(
                "user-1",
                "group-1",
                "PayVue",
                trigger
        );

        GroupAlarmContent content = (GroupAlarmContent) alarm.getContent();
        assertThat(content.getOperationUnit()).isEqualTo(AlarmOperationUnit.INVITE);
        assertThat(content.getGroupId()).isEqualTo("group-1");
        assertThat(content.getGroupName()).isEqualTo("PayVue");
        assertThat(content.getTrigger()).isEqualTo(trigger);
        assertThat(content.isRequiresAction()).isTrue();
    }

    @Test
    @DisplayName("알림 팩토리는 결제 승인 알림을 생성한다")
    void createPaymentApprovedAlarm() {
        InAppAlarm alarm = AlarmFactory.createPaymentApprovedAlarm(
                "user-1",
                "tx-1",
                2000L,
                "approver"
        );

        TransactionAlarmContent content = (TransactionAlarmContent) alarm.getContent();
        assertThat(content.getOperationUnit()).isEqualTo(AlarmOperationUnit.PAYMENT);
        assertThat(content.getTransactionId()).isEqualTo("tx-1");
        assertThat(content.getAmount()).isEqualTo(2000L);
        assertThat(content.getApprovedBy()).isEqualTo("approver");
    }

    @Test
    @DisplayName("알림 팩토리는 잔액 충전 알림을 생성한다")
    void createBalanceChargeAlarm() {
        InAppAlarm alarm = AlarmFactory.createBalanceChargeAlarm(
                "user-1",
                "tx-1",
                3000L,
                "initiator",
                "card"
        );

        TransactionAlarmContent content = (TransactionAlarmContent) alarm.getContent();
        assertThat(content.getOperationUnit()).isEqualTo(AlarmOperationUnit.PAYMENT);
        assertThat(content.getTransactionId()).isEqualTo("tx-1");
        assertThat(content.getAmount()).isEqualTo(3000L);
        assertThat(content.getInitiatedBy()).isEqualTo("initiator");
        assertThat(content.getMethod()).isEqualTo("card");
    }

    @Test
    @DisplayName("알림 팩토리는 보안 알림을 생성한다")
    void createSecurityAlarm() {
        InAppAlarm alarm = AlarmFactory.createSecurityAlarm(
                "user-1",
                "device",
                "KR",
                "127.0.0.1"
        );

        SystemAlarmContent content = (SystemAlarmContent) alarm.getContent();
        assertThat(content.getOperationUnit()).isEqualTo(AlarmOperationUnit.SECURITY);
        assertThat(content.getDevice()).isEqualTo("device");
        assertThat(content.getLocation()).isEqualTo("KR");
        assertThat(content.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(content.getReason()).isEqualTo("새 기기 로그인 감지");
    }

    @Test
    @DisplayName("알림 팩토리는 로그아웃 알림을 생성한다")
    void createLogoutAlarm() {
        InAppAlarm alarm = AlarmFactory.createLogoutAlarm(
                "user-1",
                "device",
                "manual"
        );

        SystemAlarmContent content = (SystemAlarmContent) alarm.getContent();
        assertThat(content.getOperationUnit()).isEqualTo(AlarmOperationUnit.LOGOUT);
        assertThat(content.getDevice()).isEqualTo("device");
        assertThat(content.getLocation()).isEqualTo("unknown");
        assertThat(content.getIpAddress()).isEqualTo("unknown");
        assertThat(content.getReason()).isEqualTo("manual");
    }

    @Test
    @DisplayName("알림 팩토리는 기본 템플릿 인증 이메일을 생성한다")
    void createVerificationEmailUsesDefaultTemplate() {
        EmailAlarm alarm = AlarmFactory.createVerificationEmail(
                "user@payvue.com",
                "Alice",
                "signup",
                "https://example.com/verify"
        );

        verificationEmailContent content = (verificationEmailContent) alarm.getContent();
        assertThat(content.username()).isEqualTo("Alice");
        assertThat(content.action()).isEqualTo("signup");
        assertThat(content.verifyUrl()).isEqualTo("https://example.com/verify");
        assertThat(content.emailTemplateId()).isEqualTo(EmailTemplateId.VERIFICATION);
    }

    @Test
    @DisplayName("알림 팩토리는 지정된 템플릿 인증 이메일을 생성한다")
    void createVerificationEmailWithTemplate() {
        EmailAlarm alarm = AlarmFactory.createVerificationEmail(
                "user@payvue.com",
                "Alice",
                "signup",
                "https://example.com/verify",
                EmailTemplateId.VERIFICATION
        );

        verificationEmailContent content = (verificationEmailContent) alarm.getContent();
        assertThat(content.username()).isEqualTo("Alice");
        assertThat(content.action()).isEqualTo("signup");
        assertThat(content.verifyUrl()).isEqualTo("https://example.com/verify");
        assertThat(content.emailTemplateId()).isEqualTo(EmailTemplateId.VERIFICATION);
    }
}
