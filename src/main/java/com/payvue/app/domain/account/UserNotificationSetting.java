package com.payvue.app.domain.account;

import com.payvue.supporting.user.command.FeatureKey;
import com.payvue.supporting.user.FeaturePreference;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class UserNotificationSetting implements FeaturePreference {

    private static final FeatureKey KEY = FeatureKey.of("NOTIFICATION_SETTING");

    private final boolean serviceEnabled;
    private final boolean communityEnabled;

    private UserNotificationSetting(boolean serviceEnabled, boolean communityEnabled) {
        this.serviceEnabled = serviceEnabled;
        this.communityEnabled = communityEnabled;
    }

    public static UserNotificationSetting defaults() {
        return new UserNotificationSetting(true, true);
    }

    public static UserNotificationSetting of(boolean serviceEnabled, boolean communityEnabled) {
        return new UserNotificationSetting(serviceEnabled, communityEnabled);
    }

    @Override
    public FeatureKey key() {
        return KEY;
    }
}
