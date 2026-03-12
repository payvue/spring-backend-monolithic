package com.payvue.generic.payment;

import com.payvue.app.domain.payment.PaymentProvider;
import com.payvue.generic.payment.command.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

@Configuration
@ComponentScan(basePackages = "com.payvue.app.infrastructure.pg")
public class PaymentConfiguration {

  @Bean
  public PaymentSupportRegistry paymentModuleRegistry(
      ConfigurableListableBeanFactory beanFactory
  ) {
    @SuppressWarnings("unchecked")
    Map<String, PaymentGateway<?>> gateways =
        (Map<String, PaymentGateway<?>>) (Map<?, ?>) beanFactory.getBeansOfType(PaymentGateway.class);
    @SuppressWarnings("unchecked")
    Map<String, PaymentTranslator<?>> translators =
        (Map<String, PaymentTranslator<?>>) (Map<?, ?>) beanFactory.getBeansOfType(PaymentTranslator.class);
    PaymentSupportRegistry registry = new PaymentSupportRegistry();

    PaymentGateway<PaymentProviderResponse> naverGateway =
        ProviderCaster.gatewayFor(gateways, beanFactory, PaymentProvider.NAVER, PaymentModeType.SINGLE);
    PaymentTranslator<PaymentProviderResponse> naverTranslator =
        ProviderCaster.translatorFor(translators, beanFactory, PaymentProvider.NAVER, PaymentModeType.SINGLE);
    SinglePaymentMode<PaymentProviderResponse> naverMode = new SinglePaymentMode<>();
    UnsupportedPaymentMode<PaymentProviderResponse> naverRecurringMode =
        new UnsupportedPaymentMode<>(PaymentModeType.RECURRING, "Naver 정기 결제는 아직 지원하지 않습니다.");

      registry.provider(new ProviderId(PaymentProvider.NAVER.name()))
        .gateway(new NoOpPaymentGateway(new ProviderId(PaymentProvider.NAVER.name())))
        .translator(new NoOpPaymentTranslator(new ProviderId(PaymentProvider.NAVER.name())))
        .dispatcher(PaymentCommandDispatcher.builder().build())
        .modes(modes -> modes
            .single(
                mode -> mode.strategy(new SinglePaymentMode<>())
                    .gateway(naverGateway)
                    .translator(naverTranslator)
                    .dispatcher(
                            PaymentCommandDispatcher.builder()
                                    .on(ReserveCommand.class, cmd -> naverMode.reserve(naverGateway, cmd))
                                    .on(ConfirmCommand.class, cmd -> naverMode.confirm(naverGateway, cmd))
                                    .on(CancelCommand.class, cmd -> naverMode.cancel(naverGateway, cmd))
                                    .on(HistoryCommand.class, cmd -> naverMode.history(naverGateway, cmd))
                                    .build()
                    )
            )
            .recurring(
                mode -> mode.strategy(naverRecurringMode)
                    .dispatcher(unsupportedDispatcher(naverRecurringMode, naverGateway))
            )
        );

    PaymentGateway<PaymentProviderResponse> tossGateway =
        ProviderCaster.gatewayFor(gateways, beanFactory, PaymentProvider.TOSS, PaymentModeType.VIRTUAL_ACCOUNT);
    PaymentTranslator<PaymentProviderResponse> tossTranslator =
        ProviderCaster.translatorFor(translators, beanFactory, PaymentProvider.TOSS, PaymentModeType.VIRTUAL_ACCOUNT);
    VirtualAccountPaymentMode<PaymentProviderResponse> tossVirtualMode = new VirtualAccountPaymentMode<>();
    UnsupportedPaymentMode<PaymentProviderResponse> tossSingleMode =
        new UnsupportedPaymentMode<>(PaymentModeType.SINGLE, "Toss 단건 결제는 가상계좌만 지원합니다.");
    UnsupportedPaymentMode<PaymentProviderResponse> tossRecurringMode =
        new UnsupportedPaymentMode<>(PaymentModeType.RECURRING, "Toss 정기 결제는 아직 지원하지 않습니다.");
    UnsupportedPaymentMode<PaymentProviderResponse> tossBatchMode =
        new UnsupportedPaymentMode<>(PaymentModeType.BATCH, "Toss 배치 결제는 아직 지원하지 않습니다.");

      registry.provider(new ProviderId(PaymentProvider.TOSS.name()))
        .gateway(new NoOpPaymentGateway(new ProviderId(PaymentProvider.TOSS.name())))
        .translator(new NoOpPaymentTranslator(new ProviderId(PaymentProvider.TOSS.name())))
        .dispatcher(PaymentCommandDispatcher.builder().build())
        .modes(modes -> modes
            .register(
                PaymentModeType.VIRTUAL_ACCOUNT,
                    mode -> mode.strategy(tossVirtualMode)
                    .gateway(tossGateway)
                    .translator(tossTranslator)
                    .dispatcher(
                            PaymentCommandDispatcher.builder()
                                    .on(VirtualAccountReserveCommand.class, cmd -> tossVirtualMode.virtualReserve(tossGateway, cmd))
                                    .on(ConfirmCommand.class, cmd -> tossVirtualMode.confirm(tossGateway, cmd))
                                    .on(CancelCommand.class, cmd -> tossVirtualMode.cancel(tossGateway, cmd))
                                    .on(HistoryCommand.class, cmd -> tossVirtualMode.history(tossGateway, cmd))
                                    .on(ReserveCommand.class, cmd -> tossVirtualMode.reserve(tossGateway, cmd))
                                    .build()
                    )
            )
            .single(
                mode -> mode.strategy(tossSingleMode)
                    .dispatcher(unsupportedDispatcher(tossSingleMode, tossGateway))
            )
            .recurring(
                mode -> mode.strategy(tossRecurringMode)
                    .dispatcher(unsupportedDispatcher(tossRecurringMode, tossGateway))
            )
            .batch(
                mode -> mode.strategy(tossBatchMode)
                    .dispatcher(unsupportedDispatcher(tossBatchMode, tossGateway))
            )
        );

    return registry;
  }

  private static final class ProviderCaster {

    private ProviderCaster() {}

    @SuppressWarnings("unchecked")
    private static <R extends PaymentProviderResponse> PaymentGateway<R> gatewayFor(
        Map<String, PaymentGateway<?>> gateways,
        ConfigurableListableBeanFactory beanFactory,
        PaymentProvider provider,
        PaymentModeType mode
    ) {
      List<Map.Entry<String, PaymentGateway<?>>> candidates = gateways.entrySet().stream()
          .filter(entry -> {
            PaymentGateway<?> gateway = entry.getValue();
            return Objects.equals(gateway.providerId(), new ProviderId(provider.name())) && gateway.supports(mode);
          })
          .sorted(Map.Entry.comparingByKey())
          .toList();
      if (candidates.isEmpty()) {
        throw new IllegalArgumentException("결제 게이트웨이를 찾을 수 없습니다: " + provider + "/" + mode);
      }
      Map.Entry<String, PaymentGateway<?>> primary = candidates.stream()
          .filter(entry -> beanFactory.containsBeanDefinition(entry.getKey())
              && beanFactory.getBeanDefinition(entry.getKey()).isPrimary())
          .findFirst()
          .orElse(null);
      PaymentGateway<?> selected = primary != null ? primary.getValue() : candidates.get(0).getValue();
      return (PaymentGateway<R>) selected;
    }

    @SuppressWarnings("unchecked")
    private static <R extends PaymentProviderResponse> PaymentTranslator<R> translatorFor(
        Map<String, PaymentTranslator<?>> translators,
        ConfigurableListableBeanFactory beanFactory,
        PaymentProvider provider,
        PaymentModeType mode
    ) {
      List<Map.Entry<String, PaymentTranslator<?>>> candidates = translators.entrySet().stream()
          .filter(entry -> {
            PaymentTranslator<?> translator = entry.getValue();
            return Objects.equals(translator.providerId(), new ProviderId(provider.name())) && translator.supports(mode);
          })
          .sorted(Map.Entry.comparingByKey())
          .toList();
      if (candidates.isEmpty()) {
        throw new IllegalArgumentException("결제 번역기를 찾을 수 없습니다: " + provider + "/" + mode);
      }
      Map.Entry<String, PaymentTranslator<?>> primary = candidates.stream()
          .filter(entry -> beanFactory.containsBeanDefinition(entry.getKey())
              && beanFactory.getBeanDefinition(entry.getKey()).isPrimary())
          .findFirst()
          .orElse(null);
      PaymentTranslator<?> selected = primary != null ? primary.getValue() : candidates.get(0).getValue();
      return (PaymentTranslator<R>) selected;
    }
  }

  private static <R extends PaymentProviderResponse> PaymentCommandDispatcher<R> unsupportedDispatcher(
      UnsupportedPaymentMode<R> mode,
      PaymentGateway<R> gateway
  ) {
    return PaymentCommandDispatcher.<R>builder()
        .on(ReserveCommand.class, cmd -> mode.reserve(gateway, cmd))
        .on(ConfirmCommand.class, cmd -> mode.confirm(gateway, cmd))
        .on(CancelCommand.class, cmd -> mode.cancel(gateway, cmd))
        .on(HistoryCommand.class, cmd -> mode.history(gateway, cmd))
        .build();
  }
}
