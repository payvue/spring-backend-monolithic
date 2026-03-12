package com.payvue.app.domain.alarm;

import com.payvue.generic.alarm.AlarmContent;
import com.payvue.generic.alarm.AlarmOperationUnit;
import com.payvue.generic.alarm.Trigger;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.Objects;

@Getter
@ToString
@EqualsAndHashCode
public final class GroupAlarmContent implements AlarmContent {
    private final AlarmOperationUnit operationUnit;
    private final String groupId;
    private final String groupName;
    private final Trigger trigger;
    private final boolean requiresAction;

    public GroupAlarmContent(
            @NonNull AlarmOperationUnit operationUnit,
            @NonNull String groupId,
            @NonNull String groupName,
            @NonNull Trigger trigger,
            boolean requiresAction
    ) {
        this.operationUnit = Objects.requireNonNull(operationUnit, "operationUnit is required");
        this.groupId = Objects.requireNonNull(groupId, "groupId is required");
        this.groupName = Objects.requireNonNull(groupName, "groupName is required");
        this.trigger = Objects.requireNonNull(trigger, "trigger is required");
        this.requiresAction = requiresAction;
    }

    @Override
    public String operationKey() {
        return operationUnit.key();
    }
}
