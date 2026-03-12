package com.payvue.app.presentation.rest.account;

record PaymentLimitSettingResponse(
    long dailyLimit,
    long monthlyLimit,
    boolean enableDailyLimit,
    boolean enableMonthlyLimit,
    boolean enableNotifications,
    long dailyUsed,
    long monthlyUsed
) {}
