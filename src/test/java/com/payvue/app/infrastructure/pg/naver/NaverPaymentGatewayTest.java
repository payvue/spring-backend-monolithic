package com.payvue.app.infrastructure.pg.naver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.payvue.app.infrastructure.pg.PaymentPgProperties;
import com.payvue.generic.payment.OrderItem;
import com.payvue.shared.Money;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class NaverPaymentGatewayTest {

  @Test
  @DisplayName("Naver reserve는 요청 바디와 엔드포인트를 구성한다")
  void reserveBuildsRequest() {
    WebClient webClient = mock(WebClient.class);
    WebClient.RequestBodyUriSpec postSpec = mock(WebClient.RequestBodyUriSpec.class);
    WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
    WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
    WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

    when(webClient.post()).thenReturn(postSpec);
    when(postSpec.uri(eq("/reserve"))).thenReturn(bodySpec);
    when(bodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodySpec);
    doReturn(headersSpec).when(bodySpec).bodyValue(any());
    when(headersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(NaverReserveResponse.class)).thenReturn(
        Mono.just(new NaverReserveResponse("Success", "ok", null, "order-1"))
    );

    NaverPaymentGateway gateway = new NaverPaymentGateway(webClient, properties());
    OrderItem order = new OrderItem(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), "coffee", 2, Money.of(1000L, "KRW"));

    gateway.reserve(order);

    ArgumentCaptor<NaverReserveRequest> captor = ArgumentCaptor.forClass(NaverReserveRequest.class);
    verify(bodySpec).bodyValue(captor.capture());
    NaverReserveRequest request = captor.getValue();

    assertThat(request.productName()).isEqualTo("coffee");
    assertThat(request.productCount()).isEqualTo(2);
    assertThat(request.totalPayAmount()).isEqualTo(2000L);
    assertThat(request.merchantPayKey()).isEqualTo("00000000-0000-0000-0000-000000000001");
    assertThat(request.merchantUserKey()).isEqualTo("00000000-0000-0000-0000-000000000001");
    assertThat(request.taxScopeAmount()).isEqualTo(1000L);
    assertThat(request.returnUrl()).isEqualTo("https://return/00000000-0000-0000-0000-000000000001");
  }

  @Test
  @DisplayName("Naver confirm은 form 데이터와 엔드포인트를 구성한다")
  void confirmBuildsFormRequest() {
    WebClient webClient = mock(WebClient.class);
    WebClient.RequestBodyUriSpec postSpec = mock(WebClient.RequestBodyUriSpec.class);
    WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
    WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
    WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

    when(webClient.post()).thenReturn(postSpec);
    when(postSpec.uri(eq("/confirm"))).thenReturn(bodySpec);
    when(bodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(bodySpec);
    doReturn(headersSpec).when(bodySpec).bodyValue(any());
    when(headersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(NaverApplyResponse.class)).thenReturn(Mono.just(new NaverApplyResponse("Success", "ok", "pid", "SUCCESS")));

    NaverPaymentGateway gateway = new NaverPaymentGateway(webClient, properties());
    String paymentId = UUID.randomUUID().toString();

    gateway.confirm(paymentId);

    ArgumentCaptor<MultiValueMap<String, String>> captor = ArgumentCaptor.forClass(MultiValueMap.class);
    verify(bodySpec).bodyValue(captor.capture());
    MultiValueMap<String, String> form = captor.getValue();

    assertThat(form.getFirst("paymentId")).isEqualTo(paymentId);
  }

  @Test
  @DisplayName("Naver cancel은 요청 바디와 엔드포인트를 구성한다")
  void cancelBuildsRequest() {
    WebClient webClient = mock(WebClient.class);
    WebClient.RequestBodyUriSpec postSpec = mock(WebClient.RequestBodyUriSpec.class);
    WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
    WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
    WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

    when(webClient.post()).thenReturn(postSpec);
    when(postSpec.uri(eq("/cancel"))).thenReturn(bodySpec);
    when(bodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodySpec);
    doReturn(headersSpec).when(bodySpec).bodyValue(any());
    when(headersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(NaverCancelResponse.class)).thenReturn(Mono.just(new NaverCancelResponse("Success", "ok", "pid")));

    NaverPaymentGateway gateway = new NaverPaymentGateway(webClient, properties());

    gateway.cancel("pid", "reason");

    ArgumentCaptor<NaverCancelRequest> captor = ArgumentCaptor.forClass(NaverCancelRequest.class);
    verify(bodySpec).bodyValue(captor.capture());
    NaverCancelRequest request = captor.getValue();

    assertThat(request.paymentId()).isEqualTo("pid");
    assertThat(request.cancelReason()).isEqualTo("reason");
  }

  @Test
  @DisplayName("Naver history는 요청 바디와 엔드포인트를 구성한다")
  void historyBuildsRequest() {
    WebClient webClient = mock(WebClient.class);
    WebClient.RequestBodyUriSpec postSpec = mock(WebClient.RequestBodyUriSpec.class);
    WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
    WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
    WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

    when(webClient.post()).thenReturn(postSpec);
    when(postSpec.uri(eq("/history"))).thenReturn(bodySpec);
    when(bodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodySpec);
    doReturn(headersSpec).when(bodySpec).bodyValue(any());
    when(headersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(NaverHistoryResponse.class)).thenReturn(Mono.just(new NaverHistoryResponse("Success", "ok", java.util.List.of())));

    NaverPaymentGateway gateway = new NaverPaymentGateway(webClient, properties());

    gateway.history("start", "end");

    ArgumentCaptor<NaverHistoryRequest> captor = ArgumentCaptor.forClass(NaverHistoryRequest.class);
    verify(bodySpec).bodyValue(captor.capture());
    NaverHistoryRequest request = captor.getValue();

    assertThat(request.startTime()).isEqualTo("start");
    assertThat(request.endTime()).isEqualTo("end");
  }

  private PaymentPgProperties properties() {
    PaymentPgProperties properties = new PaymentPgProperties();
    PaymentPgProperties.Naver naver = new PaymentPgProperties.Naver();
    PaymentPgProperties.Naver.Endpoints endpoints = new PaymentPgProperties.Naver.Endpoints();
    endpoints.setReserve("/reserve");
    endpoints.setConfirm("/confirm");
    endpoints.setCancel("/cancel");
    endpoints.setHistory("/history");
    naver.setEndpoints(endpoints);
    naver.setReturnUrl("https://return");
    naver.setPaymentWindowBaseUrl("https://window");
    properties.setNaver(naver);
    return properties;
  }
}
