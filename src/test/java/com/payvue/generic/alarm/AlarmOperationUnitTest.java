package com.payvue.generic.alarm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payvue.app.domain.alarm.GroupAlarmContent;
import com.payvue.app.domain.alarm.TransactionAlarmContent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AlarmOperationUnitTest {

    @Test
    @DisplayName("AlarmOperationUnit은 키로 조회할 수 있다")
    void fromKeyResolvesUnit() {
        AlarmOperationUnit unit = AlarmOperationUnit.fromKey("payment");

        assertThat(unit).isEqualTo(AlarmOperationUnit.PAYMENT);
    }

    @Test
    @DisplayName("AlarmOperationUnit은 알 수 없는 키면 예외를 던진다")
    void fromKeyThrowsWhenUnknown() {
        assertThatThrownBy(() -> AlarmOperationUnit.fromKey("missing"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("GROUP 알림은 트리거와 그룹명으로 메시지를 만든다")
    void resolveUsesTriggerAndGroupName() {
        GroupAlarmContent content = new GroupAlarmContent(
                AlarmOperationUnit.BALANCE_CHARGE,
                "group-1",
                "PayVue",
                new Trigger("Alice", "USER"),
                false
        );
        AlarmMessage message = AlarmOperationUnit.BALANCE_CHARGE.resolve(content, new Trigger("Alice", "USER"));

        assertThat(message.title()).isEqualTo("결제 알림");
        assertThat(message.message()).contains("Alice").contains("PayVue");
        assertThat(message.description()).isEqualTo("그룹 결제");
    }

    @Test
    @DisplayName("PAYMENT 알림은 금액을 포함한 메시지를 만든다")
    void resolveFormatsPaymentAmount() {
        TransactionAlarmContent content = TransactionAlarmContent.builder()
                .operationUnit(AlarmOperationUnit.PAYMENT)
                .transactionId("tx-1")
                .amount(12345L)
                .build();

        AlarmMessage message = AlarmOperationUnit.PAYMENT.resolve(content, new Trigger("SYSTEM", "SYSTEM"));

        assertThat(message.title()).isEqualTo("결제 승인");
        assertThat(message.message()).contains("12,345");
        assertThat(message.description()).isEqualTo("결제 승인");
    }
}
