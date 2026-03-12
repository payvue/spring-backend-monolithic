package com.payvue.generic.alarm;

import com.payvue.app.domain.alarm.GroupAlarmContent;
import com.payvue.app.domain.alarm.TransactionAlarmContent;

public enum AlarmOperationUnit {
    BALANCE_CHARGE(Category.GROUP, (content, trigger) -> {
        GroupAlarmContent c = (GroupAlarmContent) content;
        return AlarmMessage.of(
                "결제 알림",
                String.format("%s님이 %s에서 결제하였습니다.", trigger.name(), c.getGroupName()),
                "그룹 결제"
        );
    }),
    PAYMENT_REQUEST(Category.GROUP, (content, trigger) -> {
        GroupAlarmContent c = (GroupAlarmContent) content;
        return AlarmMessage.of(
                "결제 요청",
                String.format("%s님이 결제를 요청했습니다.", trigger.name()),
                "결제 요청"
        );
    }),
    COMMUNITY_JOINED(Category.GROUP, (content, trigger) -> {
        GroupAlarmContent c = (GroupAlarmContent) content;
        return AlarmMessage.of(
                "커뮤니티 참가",
                String.format("%s님이 '%s'에 참가했습니다.", trigger.name(), c.getGroupName()),
                "커뮤니티 참가"
        );
    }),
    COMMUNITY_INVITE_REJECTED(Category.GROUP, (content, trigger) -> {
        GroupAlarmContent c = (GroupAlarmContent) content;
        return AlarmMessage.of(
                "커뮤니티 초대 거절",
                String.format("%s님이 '%s' 초대를 거절했습니다.", trigger.name(), c.getGroupName()),
                "초대 거절"
        );
    }),
    TRANSFER(Category.GROUP, (content, trigger) -> {
        GroupAlarmContent c = (GroupAlarmContent) content;
        return AlarmMessage.of(
                "자산 전송",
                String.format("%s님이 '%s'에 자산을 전송했습니다.", trigger.name(), c.getGroupName()),
                "자산 전송"
        );
    }),
    PAYMENT_REQUEST_APPROVED(Category.GROUP, (content, trigger) -> {
        return AlarmMessage.of(
                "결제 요청 승인",
                String.format("%s님이 결제 요청을 승인했습니다.", trigger.name()),
                "결제 요청 승인"
        );
    }),
    PAYMENT_REQUEST_REJECTED(Category.GROUP, (content, trigger) -> {
        return AlarmMessage.of(
                "결제 요청 거절",
                String.format("%s님이 결제 요청을 거절했습니다.", trigger.name()),
                "결제 요청 거절"
        );
    }),
    INVITE(Category.GROUP, (content, trigger) -> {
        GroupAlarmContent c = (GroupAlarmContent) content;
        return AlarmMessage.of(
                "그룹 초대",
                String.format("%s님이 '%s' 그룹에 초대했습니다.", trigger.name(), c.getGroupName()),
                "그룹 초대"
        );
    }),

    TRANSFERCASH(Category.TRANSACTION, (content, trigger) -> {
        TransactionAlarmContent c = (TransactionAlarmContent) content;
        return AlarmMessage.of(
                "송금 완료",
                String.format("%s님이 송금했습니다.", trigger.name()),
                "송금 알림"
        );
    }),
    PAYMENT(Category.TRANSACTION, (content, trigger) -> {
        TransactionAlarmContent c = (TransactionAlarmContent) content;
        return AlarmMessage.of(
                "결제 승인",
                String.format("%,d원 결제가 승인되었습니다.", c.getAmount()),
                "결제 승인"
        );
    }),

    SECURITY(Category.SYSTEM, (content, trigger) ->
            AlarmMessage.of("보안 알림", "새로운 기기에서 로그인이 감지되었습니다.", "보안 경고")),
    LOGOUT(Category.SYSTEM, (content, trigger) ->
            AlarmMessage.of("로그아웃 알림", "기기에서 로그아웃이 발생했습니다.", "시스템 로그아웃")),

    VERIFICATION(Category.SYSTEM, (content, trigger) ->
            AlarmMessage.of("인증 알림", "요청한 작업에 대해 인증을 진행해주세요.", "인증 확인"));




    public enum Category {
        GROUP,
        TRANSACTION,
        SYSTEM
    }

    private final Category category;
    private final AlarmMessageFactory factory;

    AlarmOperationUnit(Category category, AlarmMessageFactory factory) {
        this.category = category;
        this.factory = factory;
    }

    public Category category() {
        return category;
    }

    public String key() {
        return name().toLowerCase();
    }

    public String toFrontendFormat() {
        return key();
    }

    public static AlarmOperationUnit fromKey(String key) {
        for (AlarmOperationUnit unit : values()) {
            if (unit.key().equals(key)) {
                return unit;
            }
        }
        throw new IllegalArgumentException("unknown operation key: " + key);
    }

    public AlarmMessage resolve(AlarmContent content, Trigger trigger) {
        return factory.create(content, trigger);
    }

    @FunctionalInterface
    interface AlarmMessageFactory {
        AlarmMessage create(AlarmContent content, Trigger trigger);
    }
}
