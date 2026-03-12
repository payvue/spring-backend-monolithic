package com.payvue.app.infrastructure.pg;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@SpringBootTest(
    classes = {
        PgClientConfiguration.class,
        PgClientConfigurationBootTest.CaptureConfig.class
    },
    properties = {
        "payment.pg.timeout-ms=5000",
        "payment.pg.naver.base-url=http://naver.example",
        "payment.pg.naver.client-id=naver-client",
        "payment.pg.naver.client-secret=naver-secret",
        "payment.pg.naver.chain-id=naver-chain",
        "payment.pg.toss.base-url=http://toss.example",
        "payment.pg.toss.secret-key=toss-secret"
    }
)
class PgClientConfigurationBootTest {

  @Autowired
  @Qualifier("naverPayWebClient")
  private WebClient naverWebClient;

  @Autowired
  @Qualifier("tossPayWebClient")
  private WebClient tossWebClient;

  @Autowired
  private CapturingExchangeFunction exchangeFunction;

  @Test
  @DisplayName("Naver WebClient는 기본 헤더와 baseUrl을 설정한다")
  void naverClientAddsHeaders() {
    naverWebClient.get().uri("/ping").retrieve().toBodilessEntity().block();

    ClientRequest request = exchangeFunction.lastRequest();

    assertThat(request.url().toString()).startsWith("http://naver.example");
    assertThat(request.headers().getFirst("X-Naver-Client-Id")).isEqualTo("naver-client");
    assertThat(request.headers().getFirst("X-Naver-Client-Secret")).isEqualTo("naver-secret");
    assertThat(request.headers().getFirst("X-NaverPay-Chain-Id")).isEqualTo("naver-chain");
  }

  @Test
  @DisplayName("Toss WebClient는 Basic 인증 헤더를 설정한다")
  void tossClientAddsBasicAuth() {
    tossWebClient.get().uri("/ping").retrieve().toBodilessEntity().block();

    ClientRequest request = exchangeFunction.lastRequest();
    String expected = "Basic " + Base64.getEncoder()
        .encodeToString("toss-secret:".getBytes(StandardCharsets.UTF_8));

    assertThat(request.url().toString()).startsWith("http://toss.example");
    assertThat(request.headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo(expected);
  }

  @TestConfiguration
  static class CaptureConfig {

    @Bean
    CapturingExchangeFunction capturingExchangeFunction() {
      return new CapturingExchangeFunction();
    }

    @Bean
    @Primary
    WebClient.Builder webClientBuilder(CapturingExchangeFunction exchangeFunction) {
      return WebClient.builder().exchangeFunction(exchangeFunction);
    }
  }

  static class CapturingExchangeFunction implements ExchangeFunction {
    private final List<ClientRequest> requests = new CopyOnWriteArrayList<>();

    @Override
    public Mono<ClientResponse> exchange(ClientRequest request) {
      requests.add(request);
      return Mono.just(ClientResponse.create(HttpStatus.OK).build());
    }

    ClientRequest lastRequest() {
      if (requests.isEmpty()) {
        throw new IllegalStateException("no requests captured");
      }
      return requests.get(requests.size() - 1);
    }
  }
}
