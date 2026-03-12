package com.payvue.app.domain.auth;

import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;

import java.util.HashMap;
import java.util.Map;

public enum VerificationPurpose {
    SIGNUP("회원가입", 86400) {
        @Override
        public String getIdentifier(UserAccountView view) {
            return view.email();
        }

        @Override
        public Map<String, String> buildMetadata(UserAccountView view, User user) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("email", view.email());
            return metadata;
        }
    },
    
    FIND_ID("아이디 찾기", 1800) {
        @Override
        public String getIdentifier(UserAccountView view) {
            return view.email();
        }

        @Override
        public Map<String, String> buildMetadata(UserAccountView view, User user) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("email", view.email());
            return metadata;
        }
    },
    
    RESET_PASSWORD("비밀번호 재설정", 3600) {
        @Override
        public String getIdentifier(UserAccountView view) {
            return view.email();
        }

        @Override
        public Map<String, String> buildMetadata(UserAccountView view, User user) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("email", view.email());
            metadata.put("userId", user.getId().getValue().toString());
            return metadata;
        }
    };

    private final String displayName;
    private final long defaultTtlSeconds;

    VerificationPurpose(String displayName, long defaultTtlSeconds) {
        this.displayName = displayName;
        this.defaultTtlSeconds = defaultTtlSeconds;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getDefaultTtlSeconds() {
        return defaultTtlSeconds;
    }

    public abstract String getIdentifier(UserAccountView view);

    public abstract Map<String, String> buildMetadata(UserAccountView view, User user);
}
