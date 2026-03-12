package com.payvue.app.presentation.rest.payment;

import com.payvue.app.application.pay.PayApplicationService;
import com.payvue.app.domain.payment.PaymentProvider;
import com.payvue.app.presentation.rest.ApiResponse;
import com.payvue.generic.payment.OrderItem;
import com.payvue.generic.payment.Payment;
import com.payvue.generic.payment.PaymentModeType;
import com.payvue.generic.payment.PaymentStatus;
import com.payvue.shared.Money;

import java.util.Objects;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController implements PaymentControllerDocs {

  private final PayApplicationService payApplicationService;

  @PostMapping("/{provider}/{mode}/reserve")
  public ApiResponse<PaymentReserveResponse> reserve(
          @AuthenticationPrincipal UserDetails userDetails,
          @PathVariable String provider,
          @PathVariable String mode,
          @RequestBody PaymentReserveRequest request
  ) {
    PaymentModeType modeType = PaymentModeType.from(mode);
    Payment payment = payApplicationService.reserve(
        userDetails,
        PaymentProvider.from(provider),
        modeType,
        new OrderItem(
            UUID.randomUUID(),
            request.name(),
            request.amount(),
            Money.of(request.price(), "KRW")
        ),
        request.bankCode()
    );

    if (modeType == PaymentModeType.VIRTUAL_ACCOUNT) {
      PaymentReserveResponse.VirtualAccount virtualAccount =
          new PaymentReserveResponse.VirtualAccount(
              payment.metadata().get("bankCode"),
              payment.metadata().get("accountNumber"),
              payment.metadata().get("customerName"),
              payment.metadata().get("dueDate"),
              payment.metadata().get("amount")
          );
      PaymentReserveResponse.ConfirmParams confirm =
          new PaymentReserveResponse.ConfirmParams(
              payment.getId().value(),
              payment.metadata().get("paymentKey"),
              null,
              "Success"
          );
      return ApiResponse.success(PaymentReserveResponse.virtualAccount(virtualAccount, confirm));
    }

    String reserveUrl = payment.metadata().get("redirectUrl");
    return ApiResponse.success(PaymentReserveResponse.redirect(reserveUrl));
  }

  @GetMapping("/redirect/confirm/{payId}")
  public ResponseEntity<String> confirm(
          @PathVariable String payId,
          @RequestParam(required = false) String resultCode,
          @RequestParam(required = false) String reserveId,
          @RequestParam(required = false) String paymentId
  ) {

    if (!"Success".equalsIgnoreCase(resultCode)) {
      return ResponseEntity.ok()
          .contentType(MediaType.TEXT_HTML)
          .body("<script>alert('결제 결과가 성공이 아닙니다.');window.close();</script>");
    }

    Payment confirm = payApplicationService.complete(payId, resultCode, reserveId, paymentId);
    if (Objects.equals(confirm.status(), PaymentStatus.APPROVED)) {
      return ResponseEntity.ok()
          .contentType(MediaType.TEXT_HTML)
          .body("""
              <html><body>
              <script>
                if (window.opener) {
                  window.opener.postMessage({type: 'payment:success'}, '*');
                }
                window.close();
              </script>
              </body></html>
              """);
    }
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_HTML)
        .body("<script>alert('결제 과정 중 문제가 발생했습니다.');window.close();</script>");
  }
}
