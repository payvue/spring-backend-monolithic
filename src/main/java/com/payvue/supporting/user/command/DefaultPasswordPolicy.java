package com.payvue.supporting.user.command;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * DefaultPasswordPolicy - 기본 비밀번호 정책
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DefaultPasswordPolicy implements PasswordPolicy {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 16;

    private final String password;

    public static DefaultPasswordPolicy defaults(String password) {
        return new DefaultPasswordPolicy(password);
    }

    @Override
    public void validate(String encryptedPassword, PasswordHistory history) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("비밀번호는 필수입니다.");
        }
        int length = password.length();
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            throw new IllegalArgumentException("비밀번호는 8자 이상 16자 이하로 입력해야 합니다.");
        }
        if (containsWhitespace(password)) {
            throw new IllegalArgumentException("비밀번호에는 공백을 포함할 수 없습니다.");
        }
        if (!hasLetter(password) || !hasDigitOrSpecial(password)) {
            throw new IllegalArgumentException("비밀번호는 대문자, 소문자, 숫자, 특수문자 중 2가지 이상을 포함해야 합니다.");
        }
        if (history != null && history.contains(encryptedPassword)) {
            throw new IllegalArgumentException("이전에 사용한 비밀번호는 사용할 수 없습니다.");
        }
    }

    private static boolean containsWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasLetter(String value) {
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isUpperCase(ch) || Character.isLowerCase(ch)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasDigitOrSpecial(String value) {
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isDigit(ch) || !Character.isLetter(ch)) {
                return true;
            }
        }
        return false;
    }
}
