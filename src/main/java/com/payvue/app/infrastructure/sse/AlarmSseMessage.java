package com.payvue.app.infrastructure.sse;

import com.payvue.app.domain.alarm.GroupAlarmContent;
import com.payvue.app.domain.alarm.SystemAlarmContent;
import com.payvue.app.domain.alarm.TransactionAlarmContent;
import com.payvue.generic.alarm.Alarm;
import com.payvue.generic.alarm.AlarmContent;
import com.payvue.generic.alarm.AlarmOperationUnit;

import java.util.HashMap;
import java.util.Map;

public record AlarmSseMessage(
        String id,
        String title,
        String message,
        String timestamp,
        Event event
) {

    public record Event(
            String type,
            String action,
            Meta meta,
            Map<String, Object> content
    ) {
    }

    public record Meta(
            String description,
            boolean isRead
    ) {
    }

    public static AlarmSseMessage from(Alarm alarm, boolean isRead) {
        if (alarm == null) {
            return null;
        }
        return new AlarmSseMessage(
                alarm.getId().value().toString(),
                alarm.getTitle(),
                alarm.getMessage(),
                null,
                toEvent(alarm, isRead)
        );
    }

    private static Event toEvent(Alarm alarm, boolean isRead) {
        AlarmOperationUnit unit = AlarmOperationUnit.fromKey(alarm.getContent().operationKey());
        return new Event(
                mapEventType(unit.category()),
                unit.toFrontendFormat(),
                new Meta(alarm.getDescription(), isRead),
                convertContent(alarm.getContent())
        );
    }

    private static String mapEventType(AlarmOperationUnit.Category type) {
        return type.name();
    }

    private static Map<String, Object> convertContent(AlarmContent content) {
        Map<String, Object> map = new HashMap<>();

        if (content instanceof GroupAlarmContent groupContent) {
            map.put("groupId", groupContent.getGroupId());
            map.put("groupName", groupContent.getGroupName());
            map.put("trigger", Map.of(
                    "name", groupContent.getTrigger().name(),
                    "role", groupContent.getTrigger().role()
            ));
            map.put("requiresAction", groupContent.isRequiresAction());

        } else if (content instanceof TransactionAlarmContent txContent) {
            map.put("transactionId", txContent.getTransactionId());
            map.put("amount", txContent.getAmount());

            if (txContent.getMerchant() != null) {
                map.put("merchant", txContent.getMerchant());
            }
            if (txContent.getTrigger() != null) {
                map.put("trigger", Map.of(
                        "name", txContent.getTrigger().name(),
                        "role", txContent.getTrigger().role()
                ));
            }
            if (txContent.getApprovedBy() != null) {
                map.put("approvedBy", txContent.getApprovedBy());
            }
            if (txContent.getInitiatedBy() != null) {
                map.put("initiatedBy", txContent.getInitiatedBy());
            }
            if (txContent.getRequestedBy() != null) {
                map.put("requestedBy", txContent.getRequestedBy());
            }
            if (txContent.getMethod() != null) {
                map.put("method", txContent.getMethod());
            }

        } else if (content instanceof SystemAlarmContent sysContent) {
            map.put("device", sysContent.getDevice());
            map.put("location", sysContent.getLocation());
            map.put("ip", sysContent.getIpAddress());
            map.put("reason", sysContent.getReason());
        }

        return map;
    }
}
