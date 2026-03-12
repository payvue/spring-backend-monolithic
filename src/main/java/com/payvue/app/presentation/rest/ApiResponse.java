package com.payvue.app.presentation.rest;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Clock;
import java.time.LocalDateTime;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

  private final String code;
  private final String message;

  @JsonProperty("completed_at")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  private final LocalDateTime completedAt;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private final T data;

  private ApiResponse(String code, String message, LocalDateTime completedAt, T data) {
    this.code = code;
    this.message = message;
    this.completedAt = completedAt;
    this.data = data;
  }

  public static <T> ApiResponse<T> of(ResponseStatus status, T data) {
    return new ApiResponse<>(status.code(), status.message(), now(), data);
  }

  public static <T> ApiResponse<T> of(ResponseStatus status, String message, T data) {
    String resolved = message == null || message.isBlank() ? status.message() : message;
    return new ApiResponse<>(status.code(), resolved, now(), data);
  }

  public static <T> ApiResponse<T> success(T data) {
    return of(ResponseCode.SUCCESS, data);
  }

  public static ApiResponse<Void> success() {
    return of(ResponseCode.SUCCESS, null);
  }

  public static <T> ApiResponse<T> fail(T data) {
    return of(ResponseCode.FAIL, data);
  }

  public static ApiResponse<Void> fail() {
    return of(ResponseCode.FAIL, null);
  }

  public static <T> ApiResponse<T> forbidden(T data) {
    return of(ResponseCode.FORBIDDEN, data);
  }

  public static ApiResponse<Void> forbidden() {
    return of(ResponseCode.FORBIDDEN, null);
  }

  public static <T> ApiResponse<T> unauthorized(T data) {
    return of(ResponseCode.UNAUTHORIZED, data);
  }

  public static ApiResponse<Void> unauthorized() {
    return of(ResponseCode.UNAUTHORIZED, null);
  }

  private static LocalDateTime now() {
    return LocalDateTime.now(Clock.systemDefaultZone());
  }
}
