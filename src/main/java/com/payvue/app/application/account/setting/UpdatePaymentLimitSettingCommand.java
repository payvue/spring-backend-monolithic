package com.payvue.app.application.account.setting;

public record UpdatePaymentLimitSettingCommand(
    long dailyLimit,
    long monthlyLimit,
    boolean enableDailyLimit,
    boolean enableMonthlyLimit,
    boolean enableNotifications
) {}
