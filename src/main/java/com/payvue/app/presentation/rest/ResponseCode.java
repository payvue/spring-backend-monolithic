package com.payvue.app.presentation.rest;

public enum ResponseCode implements ResponseStatus {
  SUCCESS("요청하신 응답이 완료되었습니다."),
  FAIL("요청하신 응답이 완료되지 않았습니다."),
  DUPLICATE("중복된 요청입니다."),
  INVALID_REQUEST("유효하지 않은 요청입니다."),
  FORBIDDEN("권한이 없습니다."),
  UNAUTHORIZED("인증이 필요합니다."),
  NOT_FOUND("요청한 리소스를 찾을 수 없습니다.");

  private final String message;

  ResponseCode(String message) {
    this.message = message;
  }

  @Override
  public String code() {
    return name();
  }

  @Override
  public String message() {
    return message;
  }
}
