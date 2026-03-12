package com.payvue.app.infrastructure.pg.toss;

enum BankCode {
  SHINHAN("088", "신한 은행"),
  KOOKMIN("004", "국민 은행"),
  WOORI("020", "우리 은행"),
  NONGHYUP("011", "농협 은행"),
  HANA("081", "하나 은행");

  private final String code;
  private final String bank;

  BankCode(String code, String bank) {
    this.code = code;
    this.bank = bank;
  }

  public String code() {
    return code;
  }

  public String bank() {
    return bank;
  }

  static BankCode fromCode(String code) {
    if (code == null || code.isBlank()) return null;
    for (BankCode value : values()) {
      if (value.code.equals(code)) {
        return value;
      }
    }
    return null;
  }

  static boolean isValid(String code) {
    return fromCode(code) != null;
  }

  static BankCode fromCodeOrThrow(String code) {
    BankCode found = fromCode(code);
    if (found == null) {
      throw new IllegalArgumentException("지원하지 않는 은행 코드입니다: " + code);
    }
    return found;
  }

  static BankCode orDefault(String requestedCode, String defaultCode) {
    if (requestedCode != null && !requestedCode.isBlank()) {
      return fromCodeOrThrow(requestedCode);
    }
    BankCode resolvedDefault = fromCode(defaultCode);
    if (resolvedDefault == null) {
      throw new IllegalStateException("기본 은행 코드가 유효하지 않습니다: " + defaultCode);
    }
    return resolvedDefault;
  }
}
