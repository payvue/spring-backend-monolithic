package com.payvue.app.domain.alarm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payvue.app.infrastructure.mail.EmailTemplateId;
import com.payvue.generic.alarm.AlarmOperationUnit;
import com.payvue.generic.alarm.Trigger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AlarmContentTest {

    @Test
    @DisplayName("GroupAlarmContent는 operationKey를 반환한다")
    void groupAlarmContentReturnsOperationKey() {
        GroupAlarmContent content = new GroupAlarmContent(
                AlarmOperationUnit.INVITE,
                "group-1",
                "PayVue",
                new Trigger("Alice", "USER"),
                true
        );

        assertThat(content.operationKey()).isEqualTo("invite");
    }

    @Test
    @DisplayName("TransactionAlarmContent는 builder로 생성할 수 있다")
    void transactionAlarmContentBuilder() {
        TransactionAlarmContent content = TransactionAlarmContent.builder()
                .operationUnit(AlarmOperationUnit.PAYMENT)
                .transactionId("tx-1")
                .amount(1000L)
                .approvedBy("admin")
                .build();

        assertThat(content.getOperationUnit()).isEqualTo(AlarmOperationUnit.PAYMENT);
        assertThat(content.getTransactionId()).isEqualTo("tx-1");
        assertThat(content.getAmount()).isEqualTo(1000L);
        assertThat(content.getApprovedBy()).isEqualTo("admin");
    }

    @Test
    @DisplayName("TransactionAlarmContent는 필수 값이 없으면 예외를 던진다")
    void transactionAlarmContentRequiresFields() {
        assertThatThrownBy(() -> TransactionAlarmContent.builder().build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("operationUnit");
    }

    @Test
    @DisplayName("SystemAlarmContent는 operationKey를 반환한다")
    void systemAlarmContentReturnsOperationKey() {
        SystemAlarmContent content = new SystemAlarmContent(
                AlarmOperationUnit.SECURITY,
                "device",
                "KR",
                "127.0.0.1",
                "reason"
        );

        assertThat(content.operationKey()).isEqualTo("security");
    }

    @Test
    @DisplayName("verificationEmailContent는 템플릿 변수 맵을 만든다")
    void verificationEmailContentBuildsTemplateVariables() {
        verificationEmailContent content = new verificationEmailContent(
                "Alice",
                "signup",
                "https://example.com",
                EmailTemplateId.VERIFICATION
        );

        assertThat(content.operationKey()).isEqualTo("verification");
        assertThat(content.toVariables())
                .containsEntry("userName", "Alice")
                .containsEntry("action", "signup")
                .containsEntry("verifyUrl", "https://example.com");
    }
}
