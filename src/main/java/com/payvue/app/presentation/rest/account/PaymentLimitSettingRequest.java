package com.payvue.app.presentation.rest.account;

record PaymentLimitSettingRequest(
    long dailyLimit,
    long monthlyLimit,
    boolean enableDailyLimit,
    boolean enableMonthlyLimit,
    boolean enableNotifications
) {}
