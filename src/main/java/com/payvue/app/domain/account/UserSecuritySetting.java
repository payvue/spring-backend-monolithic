package com.payvue.app.domain.account;

import com.payvue.supporting.user.command.FeatureKey;
import com.payvue.supporting.user.FeaturePreference;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class UserSecuritySetting implements FeaturePreference {

    private static final FeatureKey KEY = FeatureKey.of("SECURITY_SETTING");

    private final boolean mfaEnabled;
    private final boolean newLoginAlertEnabled;

    private UserSecuritySetting(boolean mfaEnabled, boolean newLoginAlertEnabled) {
        this.mfaEnabled = mfaEnabled;
        this.newLoginAlertEnabled = newLoginAlertEnabled;
    }

    public static UserSecuritySetting defaults() {
        return new UserSecuritySetting(false, true);
    }

    public static UserSecuritySetting of(boolean mfaEnabled, boolean newLoginAlertEnabled) {
        return new UserSecuritySetting(mfaEnabled, newLoginAlertEnabled);
    }

    @Override
    public FeatureKey key() {
        return KEY;
    }
}
