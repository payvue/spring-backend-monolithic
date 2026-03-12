package com.payvue.app.domain.account;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.payvue.supporting.user.FeaturePreference;
import com.payvue.supporting.user.command.FeatureKey;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class UserPaymentLimitSetting implements FeaturePreference {

  public static final FeatureKey KEY = FeatureKey.of("PAYMENT_LIMIT_SETTING");

  private final long dailyLimit;
  private final long monthlyLimit;
  private final boolean enableDailyLimit;
  private final boolean enableMonthlyLimit;
  private final boolean enableNotifications;

  @JsonCreator
  public UserPaymentLimitSetting(
      @JsonProperty("dailyLimit") long dailyLimit,
      @JsonProperty("monthlyLimit") long monthlyLimit,
      @JsonProperty("enableDailyLimit") boolean enableDailyLimit,
      @JsonProperty("enableMonthlyLimit") boolean enableMonthlyLimit,
      @JsonProperty("enableNotifications") boolean enableNotifications
  ) {
    this.dailyLimit = dailyLimit;
    this.monthlyLimit = monthlyLimit;
    this.enableDailyLimit = enableDailyLimit;
    this.enableMonthlyLimit = enableMonthlyLimit;
    this.enableNotifications = enableNotifications;
  }

  public static UserPaymentLimitSetting defaults() {
    return new UserPaymentLimitSetting(500000L, 10000000L, true, true, true);
  }

  public static UserPaymentLimitSetting of(
      long dailyLimit,
      long monthlyLimit,
      boolean enableDailyLimit,
      boolean enableMonthlyLimit,
      boolean enableNotifications
  ) {
    return new UserPaymentLimitSetting(
        dailyLimit,
        monthlyLimit,
        enableDailyLimit,
        enableMonthlyLimit,
        enableNotifications
    );
  }

  @Override
  public FeatureKey key() {
    return KEY;
  }
}
