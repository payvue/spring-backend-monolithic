package com.payvue.app.presentation.rest.payment;

import com.payvue.app.domain.payment.PaymentProvider;
import com.payvue.generic.payment.PaymentModeType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

  @Bean
  public Converter<String, PaymentProvider> paymentProviderConverter() {
    return new Converter<>() {
      @Override
      public PaymentProvider convert(String source) {
        return source == null ? null : PaymentProvider.from(source);
      }
    };
  }

  @Bean
  public Converter<String, PaymentModeType> paymentModeTypeConverter() {
    return new Converter<>() {
      @Override
      public PaymentModeType convert(String source) {
        return source == null ? null : PaymentModeType.valueOf(source.trim().toUpperCase());
      }
    };
  }
}
