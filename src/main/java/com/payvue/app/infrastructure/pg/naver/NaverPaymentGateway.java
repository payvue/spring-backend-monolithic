package com.payvue.app.infrastructure.pg.naver;

import com.payvue.app.infrastructure.pg.PaymentPgProperties;
import com.payvue.generic.payment.PaymentGateway;
import com.payvue.generic.payment.PaymentModeType;
import com.payvue.app.domain.payment.PaymentProvider;
import com.payvue.generic.payment.OrderItem;
import com.payvue.generic.payment.ProviderId;
import com.payvue.shared.Money;
import java.time.Duration;
import java.util.Objects;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

@Component
public class NaverPaymentGateway implements PaymentGateway<NaverPaymentResponse> {

  private final PaymentPgProperties.Naver naver;
  private final WebClient naverPayWebClient;
  private final Retry retrySpec = Retry.backoff(2, Duration.ofMillis(200))
      .filter(throwable -> true);

  public NaverPaymentGateway(
      WebClient naverPayWebClient,
      PaymentPgProperties properties
  ) {
    this.naverPayWebClient = Objects.requireNonNull(naverPayWebClient, "naverPayWebClient는 필수입니다");
    naver = properties.getNaver();
  }

  @Override
  public ProviderId providerId() {
    return new ProviderId(PaymentProvider.NAVER.name());
  }

  @Override
  public boolean supports(PaymentModeType mode) {
    return mode == PaymentModeType.SINGLE;
  }

  @Override
  public NaverPaymentResponse reserve(OrderItem order) {
    Money amount = order.totalPrice();
    NaverReserveRequest request = new NaverReserveRequest(
        order.orderId().toString(),
        order.orderId().toString(),
        order.name(),
        order.quantity(),
        amount.toLong(),
        order.unitPrice().toLong(),
        0L,
        naver.getReturnUrl() + "/" + order.orderId().toString()
    );

    return naverPayWebClient.post()
        .uri(naver.getEndpoints().getReserve())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .retrieve()
        .bodyToMono(NaverReserveResponse.class)
        .map(response -> response == null ? null : response.withOrderId(order.orderId().toString()))
        .retryWhen(retrySpec)
        .block();
  }

  @Override
  public NaverPaymentResponse confirm(String paymentId) {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("paymentId", paymentId);

    return naverPayWebClient.post()
        .uri(naver.getEndpoints().getConfirm())
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .bodyValue(formData)
        .retrieve()
        .bodyToMono(NaverApplyResponse.class)
        .retryWhen(retrySpec)
        .block();
  }

  @Override
  public NaverPaymentResponse cancel(String paymentId, String reason) {
    NaverCancelRequest cancelRequest = new NaverCancelRequest(
        paymentId,
        0L,
        reason,
        "2",
        0L,
        0L
    );

    return naverPayWebClient.post()
        .uri(naver.getEndpoints().getCancel())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(cancelRequest)
        .retrieve()
        .bodyToMono(NaverCancelResponse.class)
        .retryWhen(retrySpec)
        .block();
  }

  @Override
  public NaverPaymentResponse history(String startTime, String endTime) {
    NaverHistoryRequest historyRequest = new NaverHistoryRequest(startTime, endTime);

    return naverPayWebClient.post()
        .uri(naver.getEndpoints().getHistory())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(historyRequest)
        .retrieve()
        .bodyToMono(NaverHistoryResponse.class)
        .retryWhen(retrySpec)
        .block();
  }

}
