package com.payvue.app.infrastructure.pg;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "payment.pg")
public class PaymentPgProperties {

  private long timeoutMs = 10_000L;
  private Naver naver;
  private Toss toss;

  @Data
  public static class Naver {
    private String baseUrl;
    private String returnUrl;
    private String paymentWindowBaseUrl;
    private String clientId;
    private String clientSecret;
    private String chainId;
    private Endpoints endpoints;

    @Data
    public static class Endpoints {
      private String reserve;
      private String confirm;
      private String cancel;
      private String history;
    }
  }

  @Data
  public static class Toss {
    private String baseUrl;
    private String secretKey;
    private Endpoints endpoints;
    private Virtual virtual;

    @Data
    public static class Endpoints {
      private VirtualEndpoints virtual;
    }

    @Data
    public static class VirtualEndpoints {
      private String reserve;
      private String confirm;
      private String cancel;
      private String history;
    }

    @Data
    public static class Virtual {
      private String defaultBankCode;
      private Integer validHours;
      private String customerName;
    }
  }
}
