package com.payvue.app.infrastructure.pg;

import java.time.Duration;
import java.util.Base64;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@ImportAutoConfiguration(WebClientAutoConfiguration.class)
@EnableConfigurationProperties(PaymentPgProperties.class)
public class PgClientConfiguration {

  @Bean
  public WebClient naverPayWebClient(
      WebClient.Builder builder,
      PaymentPgProperties properties
  ) {
    PaymentPgProperties.Naver cfg = properties.getNaver();
    return build(builder, cfg.getBaseUrl(), properties.getTimeoutMs())
        .mutate()
        .defaultHeader("X-Naver-Client-Id", cfg.getClientId())
        .defaultHeader("X-Naver-Client-Secret", cfg.getClientSecret())
        .defaultHeader("X-NaverPay-Chain-Id", cfg.getChainId())
        .build();
  }

  @Bean
  public WebClient tossPayWebClient(
      WebClient.Builder builder,
      PaymentPgProperties properties
  ) {
    PaymentPgProperties.Toss cfg = properties.getToss();
    String credential = Base64.getEncoder().encodeToString((cfg.getSecretKey() + ":").getBytes());
    return build(builder, cfg.getBaseUrl(), properties.getTimeoutMs())
        .mutate()
        .defaultHeaders(
                headers -> headers.setBasicAuth(credential)
        )
        .build();
  }

  private WebClient build(WebClient.Builder builder, String baseUrl, long timeoutMs) {
    HttpClient httpClient = HttpClient.create()
        .responseTimeout(Duration.ofMillis(timeoutMs));

    return builder
        .baseUrl(baseUrl)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .exchangeStrategies(ExchangeStrategies.builder()
            .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(512 * 1024))
            .build())
        .build();
  }
}
