package com.payvue.generic.alarm;

import com.payvue.app.domain.alarm.*;
import com.payvue.app.infrastructure.mail.EmailTemplateId;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AlarmFactory {

    public static InAppAlarm createGroupPaymentAlarm(
            String userId,
            String groupId,
            String groupName,
            Trigger trigger,
            Long amount,
            String merchant
    ) {

        AlarmOperationUnit unit = AlarmOperationUnit.BALANCE_CHARGE;
        GroupAlarmContent content = new GroupAlarmContent(unit, groupId, groupName, trigger, false);
        AlarmMessage message = unit.resolve(content, trigger);

        return new InAppAlarm(
                AlarmId.generate(),
                Recipient.ofUser(userId),
                message.title(),
                message.message(),
                message.description(),
                content
        );
    }

    public static InAppAlarm createGroupPaymentRequestAlarm(
            String userId,
            String groupId,
            String groupName,
            Trigger trigger,
            Long amount,
            String merchant
    ) {
        AlarmOperationUnit unit = AlarmOperationUnit.PAYMENT_REQUEST;
        GroupAlarmContent content = new GroupAlarmContent(unit, groupId, groupName, trigger, false);
        AlarmMessage message = unit.resolve(content, trigger);

        return new InAppAlarm(
                AlarmId.generate(),
                Recipient.ofUser(userId),
                message.title(),
                message.message(),
                message.description(),
                content
        );
    }

    public static InAppAlarm createPaymentRequestApprovedAlarm(
            String userId,
            String groupId,
            String groupName,
            Trigger trigger
    ) {
        AlarmOperationUnit unit = AlarmOperationUnit.PAYMENT_REQUEST_APPROVED;
        GroupAlarmContent content = new GroupAlarmContent(unit, groupId, groupName, trigger, false);
        AlarmMessage message = unit.resolve(content, trigger);

        return new InAppAlarm(
                AlarmId.generate(),
                Recipient.ofUser(userId),
                message.title(),
                message.message(),
                message.description(),
                content
        );
    }

    public static InAppAlarm createPaymentRequestRejectedAlarm(
            String userId,
            String groupId,
            String groupName,
            Trigger trigger
    ) {
        AlarmOperationUnit unit = AlarmOperationUnit.PAYMENT_REQUEST_REJECTED;
        GroupAlarmContent content = new GroupAlarmContent(unit, groupId, groupName, trigger, false);
        AlarmMessage message = unit.resolve(content, trigger);

        return new InAppAlarm(
                AlarmId.generate(),
                Recipient.ofUser(userId),
                message.title(),
                message.message(),
                message.description(),
                content
        );
    }

    public static InAppAlarm createGroupTransferAlarm(
            String userId,
            String groupId,
            String groupName,
            Trigger trigger,
            String recipient,
            Long amount
    ) {
        AlarmOperationUnit unit = AlarmOperationUnit.TRANSFER;
        GroupAlarmContent content = new GroupAlarmContent(unit, groupId, groupName, trigger, false);
        AlarmMessage message = unit.resolve(content, trigger);

        return new InAppAlarm(
                AlarmId.generate(),
                Recipient.ofUser(userId),
                message.title(),
                message.message(),
                message.description(),
                content
        );
    }

    public static InAppAlarm createGroupInvitationAlarm(
            String userId,
            String groupId,
            String groupName,
            Trigger trigger
    ) {
        AlarmOperationUnit unit = AlarmOperationUnit.INVITE;
        GroupAlarmContent content = new GroupAlarmContent(unit, groupId, groupName, trigger, true);
        AlarmMessage message = unit.resolve(content, trigger);

        return new InAppAlarm(
                AlarmId.generate(),
                Recipient.ofUser(userId),
                message.title(),
                message.message(),
                message.description(),
                content
        );
    }

    public static InAppAlarm createCommunityJoinAlarm(
            String userId,
            String groupId,
            String groupName,
            Trigger trigger
    ) {
        AlarmOperationUnit unit = AlarmOperationUnit.COMMUNITY_JOINED;
        GroupAlarmContent content = new GroupAlarmContent(unit, groupId, groupName, trigger, false);
        AlarmMessage message = unit.resolve(content, trigger);

        return new InAppAlarm(
                AlarmId.generate(),
                Recipient.ofUser(userId),
                message.title(),
                message.message(),
                message.description(),
                content
        );
    }

    public static InAppAlarm createCommunityInviteRejectedAlarm(
            String userId,
            String groupId,
            String groupName,
            Trigger trigger
    ) {
        AlarmOperationUnit unit = AlarmOperationUnit.COMMUNITY_INVITE_REJECTED;
        GroupAlarmContent content = new GroupAlarmContent(unit, groupId, groupName, trigger, false);
        AlarmMessage message = unit.resolve(content, trigger);

        return new InAppAlarm(
                AlarmId.generate(),
                Recipient.ofUser(userId),
                message.title(),
                message.message(),
                message.description(),
                content
        );
    }

    public static InAppAlarm createPaymentApprovedAlarm(
            String userId,
            String transactionId,
            Long amount,
            String approvedBy
    ) {
        AlarmOperationUnit unit = AlarmOperationUnit.PAYMENT;
        TransactionAlarmContent content = TransactionAlarmContent.builder()
                .operationUnit(unit)
                .transactionId(transactionId)
                .amount(amount)
                .approvedBy(approvedBy)
                .build();

        AlarmMessage message = unit.resolve(
                content,
                new Trigger(approvedBy, "SYSTEM")
        );

        return new InAppAlarm(
                AlarmId.generate(),
                Recipient.ofUser(userId),
                message.title(),
                message.message(),
                message.description(),
                content
        );
    }

    public static InAppAlarm createBalanceChargeAlarm(
            String userId,
            String transactionId,
            Long amount,
            String initiatedBy,
            String method
    ) {
        AlarmOperationUnit unit = AlarmOperationUnit.PAYMENT;
        TransactionAlarmContent content = TransactionAlarmContent.builder()
                .operationUnit(unit)
                .transactionId(transactionId)
                .amount(amount)
                .initiatedBy(initiatedBy)
                .method(method)
                .build();

        String initiator = initiatedBy != null ? initiatedBy : "SYSTEM";
        AlarmMessage message = unit.resolve(
                content,
                new Trigger(initiator, "SYSTEM")
        );

        return new InAppAlarm(
                AlarmId.generate(),
                Recipient.ofUser(userId),
                message.title(),
                message.message(),
                message.description(),
                content
        );
    }

    public static InAppAlarm createTransferCashAlarm(
            String userId,
            String transactionId,
            Long amount,
            String initiatedBy
    ) {
        AlarmOperationUnit unit = AlarmOperationUnit.TRANSFERCASH;
        TransactionAlarmContent content = TransactionAlarmContent.builder()
                .operationUnit(unit)
                .transactionId(transactionId)
                .amount(amount)
                .initiatedBy(initiatedBy)
                .build();

        String initiator = initiatedBy != null ? initiatedBy : "SYSTEM";
        AlarmMessage message = unit.resolve(
                content,
                new Trigger(initiator, "SYSTEM")
        );

        return new InAppAlarm(
                AlarmId.generate(),
                Recipient.ofUser(userId),
                message.title(),
                message.message(),
                message.description(),
                content
        );
    }

    public static InAppAlarm createSecurityAlarm(
            String userId,
            String device,
            String location,
            String ipAddress
    ) {
        AlarmOperationUnit unit = AlarmOperationUnit.SECURITY;
        SystemAlarmContent content = new SystemAlarmContent(
                unit,
                device,
                location,
                ipAddress,
                "새 기기 로그인 감지"
        );
        AlarmMessage message = unit.resolve(
                content,
                new Trigger(userId, "SYSTEM")
        );

        return new InAppAlarm(
                AlarmId.generate(),
                Recipient.ofUser(userId),
                message.title(),
                message.message(),
                message.description(),
                content
        );
    }

    public static InAppAlarm createLogoutAlarm(
            String userId,
            String device,
            String reason
    ) {
        AlarmOperationUnit unit = AlarmOperationUnit.LOGOUT;
        SystemAlarmContent content = new SystemAlarmContent(
                unit,
                device,
                "unknown",
                "unknown",
                reason
        );
        AlarmMessage message = unit.resolve(
                content,
                new Trigger(userId, "SYSTEM")
        );

        return new InAppAlarm(
                AlarmId.generate(),
                Recipient.ofUser(userId),
                message.title(),
                message.message(),
                message.description(),
                content
        );
    }

    public static EmailAlarm createVerificationEmail(
            String email,
            String userName,
            String action,
            String verifyUrl
    ) {
        return createVerificationEmail(email, userName, action, verifyUrl, EmailTemplateId.VERIFICATION);
    }

    public static EmailAlarm createVerificationEmail(
            String email,
            String userName,
            String action,
            String verifyUrl,
            EmailTemplateId templateId
    ) {

        AlarmOperationUnit unit = AlarmOperationUnit.SECURITY;
        verificationEmailContent content = new verificationEmailContent(userName, action, verifyUrl, templateId);
        AlarmMessage message = unit.resolve(
                content,
                new Trigger(userName, "SYSTEM")
        );

        return new EmailAlarm(
                AlarmId.generate(),
                Recipient.ofUser(email),
                message.title(),
                message.message(),
                message.description(),
                content
        );
    }
}
