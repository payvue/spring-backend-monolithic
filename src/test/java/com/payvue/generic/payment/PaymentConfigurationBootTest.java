package com.payvue.generic.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.payvue.app.domain.payment.PaymentProvider;
import com.payvue.app.infrastructure.pg.naver.NaverReserveResponse;
import com.payvue.app.infrastructure.pg.naver.NaverPaymentGateway;
import com.payvue.app.infrastructure.pg.naver.NaverPaymentTranslator;
import com.payvue.app.infrastructure.pg.toss.TossPaymentTranslator;
import com.payvue.app.infrastructure.pg.toss.VirtualAccountTossGateway;
import com.payvue.generic.payment.command.ReserveCommand;
import com.payvue.generic.payment.command.VirtualAccountReserveCommand;
import com.payvue.shared.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootTest(classes = {
    PaymentConfiguration.class,
    PaymentConfigurationBootTest.MockConfig.class
},
    properties = "spring.main.allow-bean-definition-overriding=true")
class PaymentConfigurationBootTest {

  @Autowired
  private PaymentSupportRegistry registry;

  @Autowired
  private NaverPaymentGateway naverPaymentGateway;

  @Autowired
  private NaverPaymentTranslator naverPaymentTranslator;

  @Autowired
  private VirtualAccountTossGateway tossGateway;

  @Autowired
  private TossPaymentTranslator tossPaymentTranslator;

  @Test
  @DisplayName("PaymentConfiguration은 NAVER SINGLE 모듈을 등록한다")
  void registersNaverSingleModule() {
    PaymentContext<?> context = registry.resolve(new ProviderId(PaymentProvider.NAVER.name()), PaymentModeType.SINGLE);

    assertThat(context.providerId()).isEqualTo(new ProviderId(PaymentProvider.NAVER.name()));
    assertThat(context.modeType()).isEqualTo(PaymentModeType.SINGLE);
  }

  @Test
  @DisplayName("PaymentConfiguration은 TOSS VIRTUAL_ACCOUNT 모듈을 등록한다")
  void registersTossVirtualAccountModule() {
    PaymentContext<?> context = registry.resolve(new ProviderId(PaymentProvider.TOSS.name()), PaymentModeType.VIRTUAL_ACCOUNT);

    assertThat(context.providerId()).isEqualTo(new ProviderId(PaymentProvider.TOSS.name()));
    assertThat(context.modeType()).isEqualTo(PaymentModeType.VIRTUAL_ACCOUNT);
  }

  @Test
  @DisplayName("지원하지 않는 모드는 UnsupportedPaymentMode로 처리된다")
  void unsupportedModesThrow() {
    PaymentContext<?> naverRecurring = registry.resolve(new ProviderId(PaymentProvider.NAVER.name()), PaymentModeType.RECURRING);

    assertThatThrownBy(() -> naverRecurring.execute(new ReserveCommand(new OrderItem(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), "coffee", 1, Money.of(1000L, "KRW")))))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("Naver 정기 결제는 아직 지원하지 않습니다");

    PaymentContext<?> tossSingle = registry.resolve(new ProviderId(PaymentProvider.TOSS.name()), PaymentModeType.SINGLE);

    assertThatThrownBy(() -> tossSingle.execute(new ReserveCommand(new OrderItem(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), "coffee", 1, Money.of(1000L, "KRW")))))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("Toss 단건 결제는 가상계좌만 지원합니다");
  }

  @Test
  @DisplayName("NAVER 단건 결제는 게이트웨이와 번역기를 호출한다")
  void naverReserveInvokesGatewayAndTranslator() {
    PaymentContext<?> context = registry.resolve(new ProviderId(PaymentProvider.NAVER.name()), PaymentModeType.SINGLE);
    NaverReserveResponse response = new NaverReserveResponse("Success", "ok", null, "order-1");
    Payment payment = Payment.of(
        PaymentStatus.PENDING,
        PaymentModeType.SINGLE,
        new ProviderId(PaymentProvider.NAVER.name()),
        "external",
        java.util.Map.of()
    );
    PaymentTranslator translator = (PaymentTranslator) naverPaymentTranslator;

    when(naverPaymentGateway.reserve(any())).thenReturn(response);
    when(translator.translate(any())).thenReturn(payment);

    Payment result = context.execute(new ReserveCommand(new OrderItem(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), "coffee", 1, Money.of(1000L, "KRW"))));

    verify(naverPaymentGateway).reserve(any());
    verify(translator).translate(any());
    assertThat(result).isEqualTo(payment);
  }

  @Test
  @DisplayName("TOSS 가상계좌 예약은 게이트웨이와 번역기를 호출한다")
  void tossVirtualReserveInvokesGatewayAndTranslator() {
    PaymentContext<?> context = registry.resolve(new ProviderId(PaymentProvider.TOSS.name()), PaymentModeType.VIRTUAL_ACCOUNT);
    VirtualAccountTossGateway.TossPaymentResponse response = new VirtualAccountTossGateway.TossPaymentResponse(
        "READY",
        "pay-1",
        "order-1",
        1000,
        "VIRTUAL_ACCOUNT"
    );
    Payment payment = Payment.of(
        PaymentStatus.PENDING,
        PaymentModeType.VIRTUAL_ACCOUNT,
        new ProviderId(PaymentProvider.TOSS.name()),
        "pay-1",
        java.util.Map.of()
    );

    when(tossGateway.reserveVirtual(any(), any())).thenReturn(response);
    when(tossPaymentTranslator.translate(any())).thenReturn(payment);

    Payment result = context.execute(new VirtualAccountReserveCommand(
        new OrderItem(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), "coffee", 1, Money.of(1000L, "KRW")),
        "088"
    ));

    verify(tossGateway).reserveVirtual(any(), any());
    verify(tossPaymentTranslator).translate(any());
    assertThat(result).isEqualTo(payment);
  }

  @TestConfiguration
  static class MockConfig {

    @Bean
    @Primary
    NaverPaymentGateway naverPaymentGateway() {
      NaverPaymentGateway gateway = org.mockito.Mockito.mock(NaverPaymentGateway.class);
      when(gateway.providerId()).thenReturn(new ProviderId(PaymentProvider.NAVER.name()));
      when(gateway.supports(PaymentModeType.SINGLE)).thenReturn(true);
      return gateway;
    }

    @Bean
    @Primary
    NaverPaymentTranslator naverPaymentTranslator() {
      NaverPaymentTranslator translator = org.mockito.Mockito.mock(NaverPaymentTranslator.class);
      when(translator.providerId()).thenReturn(new ProviderId(PaymentProvider.NAVER.name()));
      when(translator.supports(PaymentModeType.SINGLE)).thenReturn(true);
      return translator;
    }

    @Bean
    @Primary
    VirtualAccountTossGateway tossGateway() {
      VirtualAccountTossGateway gateway = org.mockito.Mockito.mock(VirtualAccountTossGateway.class);
      when(gateway.providerId()).thenReturn(new ProviderId(PaymentProvider.TOSS.name()));
      when(gateway.supports(PaymentModeType.VIRTUAL_ACCOUNT)).thenReturn(true);
      return gateway;
    }

    @Bean
    @Primary
    TossPaymentTranslator tossPaymentTranslator() {
      TossPaymentTranslator translator = org.mockito.Mockito.mock(TossPaymentTranslator.class);
      when(translator.providerId()).thenReturn(new ProviderId(PaymentProvider.TOSS.name()));
      when(translator.supports(PaymentModeType.VIRTUAL_ACCOUNT)).thenReturn(true);
      return translator;
    }
  }
}
