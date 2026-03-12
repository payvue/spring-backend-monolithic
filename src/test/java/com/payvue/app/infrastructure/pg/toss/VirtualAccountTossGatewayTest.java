package com.payvue.app.infrastructure.pg.toss;

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
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class VirtualAccountTossGatewayTest {

  @Test
  @DisplayName("가상계좌 예약은 요청 바디와 엔드포인트를 구성한다")
  void reserveVirtualBuildsRequest() {
    WebClient webClient = mock(WebClient.class);
    WebClient.RequestBodyUriSpec postSpec = mock(WebClient.RequestBodyUriSpec.class);
    WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
    WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
    WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

    when(webClient.post()).thenReturn(postSpec);
    when(postSpec.uri(eq("/virtual"))).thenReturn(bodySpec);
    when(bodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodySpec);
    doReturn(headersSpec).when(bodySpec).bodyValue(any());
    when(headersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(TossVirtualAccountResponse.class)).thenReturn(
        Mono.just(new TossVirtualAccountResponse("READY", "pay-1", "order-1", 1000, null))
    );

    VirtualAccountTossGateway gateway = new VirtualAccountTossGateway(webClient, properties());

    gateway.reserveVirtual(new OrderItem(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), "coffee", 1, Money.of(1000L, "KRW")), "088");

    ArgumentCaptor<TossVirtualAccountRequest> captor = ArgumentCaptor.forClass(TossVirtualAccountRequest.class);
    verify(bodySpec).bodyValue(captor.capture());
    TossVirtualAccountRequest request = captor.getValue();

    assertThat(request.method()).isEqualTo("VIRTUAL_ACCOUNT");
    assertThat(request.bank()).isEqualTo("088");
    assertThat(request.amount()).isEqualTo(1000L);
    assertThat(request.orderId()).isEqualTo("00000000-0000-0000-0000-000000000001");
    assertThat(request.orderName()).isEqualTo("coffee");
    assertThat(request.customerName()).isEqualTo("PayVue");
    assertThat(request.validHours()).isEqualTo(24);
  }

  @Test
  @DisplayName("가상계좌 예약은 bankCode가 없으면 기본값을 사용한다")
  void reserveVirtualUsesDefaultBankWhenMissing() {
    WebClient webClient = mock(WebClient.class);
    WebClient.RequestBodyUriSpec postSpec = mock(WebClient.RequestBodyUriSpec.class);
    WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
    WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
    WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

    when(webClient.post()).thenReturn(postSpec);
    when(postSpec.uri(eq("/virtual"))).thenReturn(bodySpec);
    when(bodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodySpec);
    doReturn(headersSpec).when(bodySpec).bodyValue(any());
    when(headersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(TossVirtualAccountResponse.class)).thenReturn(
        Mono.just(new TossVirtualAccountResponse("READY", "pay-1", "order-1", 1000, null))
    );

    VirtualAccountTossGateway gateway = new VirtualAccountTossGateway(webClient, properties());

    gateway.reserveVirtual(
        new OrderItem(UUID.fromString("00000000-0000-0000-0000-000000000001"), "coffee", 1, Money.of(1000L, "KRW")),
        null
    );

    ArgumentCaptor<TossVirtualAccountRequest> captor = ArgumentCaptor.forClass(TossVirtualAccountRequest.class);
    verify(bodySpec).bodyValue(captor.capture());

    assertThat(captor.getValue().bank()).isEqualTo("088");
  }

  @Test
  @DisplayName("Toss confirm은 요청 바디와 엔드포인트를 구성한다")
  void confirmBuildsRequest() {
    WebClient webClient = mock(WebClient.class);
    WebClient.RequestHeadersUriSpec<?> getSpec = mock(WebClient.RequestHeadersUriSpec.class);
    WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
    WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

    doReturn(getSpec).when(webClient).get();
    doReturn(headersSpec).when(getSpec).uri(eq("/confirm"), eq("pay-1"));
    when(headersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(VirtualAccountTossGateway.TossPaymentResponse.class)).thenReturn(
        Mono.just(new VirtualAccountTossGateway.TossPaymentResponse("DONE", "pay-1", "order-1", 1000, "CARD"))
    );

    VirtualAccountTossGateway gateway = new VirtualAccountTossGateway(webClient, properties());
    String paymentId = "pay-1";

    gateway.confirm(paymentId);

    verify(getSpec).uri(eq("/confirm"), eq("pay-1"));
  }

  @Test
  @DisplayName("Toss cancel은 요청 바디와 엔드포인트를 구성한다")
  void cancelBuildsRequest() {
    WebClient webClient = mock(WebClient.class);
    WebClient.RequestBodyUriSpec postSpec = mock(WebClient.RequestBodyUriSpec.class);
    WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
    WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
    WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

    when(webClient.post()).thenReturn(postSpec);
    when(postSpec.uri(eq("/cancel"), eq("pay-1"))).thenReturn(bodySpec);
    when(bodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodySpec);
    doReturn(headersSpec).when(bodySpec).bodyValue(any());
    when(headersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(VirtualAccountTossGateway.TossPaymentResponse.class)).thenReturn(
        Mono.just(new VirtualAccountTossGateway.TossPaymentResponse("CANCELED", "pay-1", "order-1", 1000, "CARD"))
    );

    VirtualAccountTossGateway gateway = new VirtualAccountTossGateway(webClient, properties());

    gateway.cancel("pay-1", "reason");

    ArgumentCaptor<TossCancelRequest> captor = ArgumentCaptor.forClass(TossCancelRequest.class);
    verify(bodySpec).bodyValue(captor.capture());
    TossCancelRequest request = captor.getValue();

    assertThat(request.cancelReason()).isEqualTo("reason");
  }

  @Test
  @DisplayName("Toss history는 요청 엔드포인트를 구성한다")
  void historyBuildsRequest() {
    WebClient webClient = mock(WebClient.class);
    WebClient.RequestHeadersUriSpec<?> getSpec = mock(WebClient.RequestHeadersUriSpec.class);
    WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
    WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

    doReturn(getSpec).when(webClient).get();
    doReturn(headersSpec).when(getSpec).uri(eq("/history/{paymentKey}"), eq("pay-1"));
    when(headersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(VirtualAccountTossGateway.TossPaymentResponse.class)).thenReturn(
        Mono.just(new VirtualAccountTossGateway.TossPaymentResponse("DONE", "pay-1", "order-1", 1000, "CARD"))
    );

    VirtualAccountTossGateway gateway = new VirtualAccountTossGateway(webClient, properties());

    gateway.history("pay-1", "ignored");

    verify(getSpec).uri(eq("/history/{paymentKey}"), eq("pay-1"));
  }

  private PaymentPgProperties properties() {
    PaymentPgProperties properties = new PaymentPgProperties();
    PaymentPgProperties.Toss toss = new PaymentPgProperties.Toss();
    PaymentPgProperties.Toss.Endpoints endpoints = new PaymentPgProperties.Toss.Endpoints();
    PaymentPgProperties.Toss.VirtualEndpoints virtualEndpoints = new PaymentPgProperties.Toss.VirtualEndpoints();
    virtualEndpoints.setReserve("/virtual");
    virtualEndpoints.setConfirm("/confirm");
    virtualEndpoints.setCancel("/cancel");
    virtualEndpoints.setHistory("/history/{paymentKey}");
    endpoints.setVirtual(virtualEndpoints);
    toss.setEndpoints(endpoints);
    PaymentPgProperties.Toss.Virtual virtual = new PaymentPgProperties.Toss.Virtual();
    virtual.setDefaultBankCode("088");
    virtual.setCustomerName("PayVue");
    virtual.setValidHours(24);
    toss.setVirtual(virtual);
    properties.setToss(toss);
    return properties;
  }
}
