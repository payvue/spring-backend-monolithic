package com.payvue.app.presentation.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(IllegalStateException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> handleIllegalState(IllegalStateException ex) {
    String message = ex.getMessage() == null ? ResponseCode.INVALID_REQUEST.message() : ex.getMessage();
    return ApiResponse.of(ResponseCode.INVALID_REQUEST, message, null);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException ex) {
    String message = ex.getMessage() == null ? ResponseCode.INVALID_REQUEST.message() : ex.getMessage();
    return ApiResponse.of(ResponseCode.INVALID_REQUEST, message, null);
  }
}
