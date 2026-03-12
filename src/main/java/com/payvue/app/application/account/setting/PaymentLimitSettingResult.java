package com.payvue.app.application.account.setting;

public record PaymentLimitSettingResult(
    long dailyLimit,
    long monthlyLimit,
    boolean enableDailyLimit,
    boolean enableMonthlyLimit,
    boolean enableNotifications,
    long dailyUsed,
    long monthlyUsed
) {}
