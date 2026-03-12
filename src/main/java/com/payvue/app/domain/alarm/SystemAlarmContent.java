package com.payvue.app.domain.alarm;

import com.payvue.generic.alarm.AlarmContent;
import com.payvue.generic.alarm.AlarmOperationUnit;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.Objects;

@Getter
@ToString
@EqualsAndHashCode
public final class SystemAlarmContent implements AlarmContent {
    private final AlarmOperationUnit operationUnit;
    private final String device;
    private final String location;
    private final String ipAddress;
    private final String reason;

    public SystemAlarmContent(
            @NonNull AlarmOperationUnit operationUnit,
            @NonNull String device,
            @NonNull String location,
            @NonNull String ipAddress,
            @NonNull String reason
    ) {
        this.operationUnit = Objects.requireNonNull(operationUnit, "operationUnit is required");
        this.device = Objects.requireNonNull(device, "device is required");
        this.location = Objects.requireNonNull(location, "location is required");
        this.ipAddress = Objects.requireNonNull(ipAddress, "ipAddress is required");
        this.reason = Objects.requireNonNull(reason, "reason is required");
    }

    @Override
    public String operationKey() {
        return operationUnit.key();
    }
}
