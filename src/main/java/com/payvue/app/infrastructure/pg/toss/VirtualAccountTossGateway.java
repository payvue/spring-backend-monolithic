package com.payvue.app.infrastructure.pg.toss;

import com.payvue.app.infrastructure.pg.PaymentPgProperties;
import com.payvue.generic.payment.*;
import com.payvue.app.domain.payment.PaymentProvider;
import com.payvue.shared.Money;
import java.time.Duration;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
public class VirtualAccountTossGateway extends VirtualAccountCapableGateway<PaymentProviderResponse> {

  private final WebClient tossWebClient;
  private final PaymentPgProperties.Toss toss;
  private final Retry retrySpec = Retry
          .fixedDelay(3, Duration.ofSeconds(1))
          .filter(throwable -> !(throwable instanceof IllegalStateException));

  public VirtualAccountTossGateway(
          WebClient tossPayWebClient,
          PaymentPgProperties properties
  ) {
    toss = properties.getToss();
    this.tossWebClient = tossPayWebClient;
  }

  @Override
  public ProviderId providerId() {
    return new ProviderId(PaymentProvider.TOSS.name());
  }

  @Override
  public boolean supports(PaymentModeType mode) {
    return mode == PaymentModeType.VIRTUAL_ACCOUNT;
  }

  @Override
  public PaymentProviderResponse reserveVirtual(OrderItem order, String bankCode) {
    Money amount = order.totalPrice();
    BankCode bankEnum = BankCode.orDefault(bankCode, defaultBankCode());
    String orderId = order.orderId().toString();
    String orderName = order.name();
    TossVirtualAccountRequest request = TossVirtualAccountRequest.create(
            bankEnum.code(),
            amount.toLong(),
            orderId,
            orderName,
            toss.getVirtual().getCustomerName(),
            toss.getVirtual().getValidHours()
    );

    return tossWebClient.post()
            .uri(toss.getEndpoints().getVirtual().getReserve())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(TossVirtualAccountResponse.class)
            .retryWhen(retrySpec)
            .block();
  }

  @Override
  protected String defaultBankCode() {
    return toss.getVirtual().getDefaultBankCode();
  }

  @Override
  protected boolean supportsBankCode(String bankCode) {
    return BankCode.isValid(bankCode);
  }

  @Override
  public PaymentProviderResponse confirm(String paymentId) {
    return tossWebClient.get()
            .uri(toss.getEndpoints().getVirtual().getConfirm(), paymentId)
            .retrieve()
            .bodyToMono(TossPaymentResponse.class)
            .flatMap(response -> {
              if (response == null || response.status() == null) {
                return Mono.error(new IllegalStateException("결제 상태를 확인할 수 없습니다."));
              }
              if (!"DONE".equalsIgnoreCase(response.status())) {
                return Mono.error(new IllegalStateException("가상 계좌 입금이 확인되지 않았습니다."));
              }
              return Mono.just(response);
            })
            .retryWhen(retrySpec)
            .block();
  }

  @Override
  public PaymentProviderResponse cancel(String paymentId, String reason) {
    TossCancelRequest body = new TossCancelRequest(reason);
    return tossWebClient.post()
            .uri(toss.getEndpoints().getVirtual().getCancel(), paymentId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(TossPaymentResponse.class)
            .retryWhen(retrySpec)
            .block();
  }

  @Override
  public PaymentProviderResponse history(String startTime, String endTime) {
    String paymentKey = startTime;
    return tossWebClient.get()
            .uri(toss.getEndpoints().getVirtual().getHistory(), paymentKey)
            .retrieve()
            .bodyToMono(TossPaymentResponse.class)
            .retryWhen(retrySpec)
            .block();
  }

  public record TossPaymentResponse(
          String status,
          String paymentKey,
          String orderId,
          Integer totalAmount,
          String method
  ) implements PaymentProviderResponse {}
}
