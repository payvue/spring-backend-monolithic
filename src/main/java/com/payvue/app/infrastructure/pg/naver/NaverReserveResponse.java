package com.payvue.app.infrastructure.pg.naver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverReserveResponse(
    String code,
    String message,
    @JsonProperty("body") Body body,
    String orderId
) implements NaverPaymentResponse {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Body(
      @JsonProperty("reserveId") String reserveId
  ) {
  }

  public NaverReserveResponse withOrderId(String orderId) {
    return new NaverReserveResponse(code, message, body, orderId);
  }
}
