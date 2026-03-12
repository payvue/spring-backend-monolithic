package com.payvue.app.domain.payment;

import com.payvue.generic.payment.*;
import com.payvue.generic.payment.command.CancelCommand;
import com.payvue.generic.payment.command.ConfirmCommand;
import com.payvue.generic.payment.command.HistoryCommand;
import com.payvue.generic.payment.command.ReserveCommand;
import com.payvue.generic.payment.command.VirtualAccountReserveCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentSupportRegistry paymentSupportRegistry;
  private final PaymentRepository paymentRepository;

  public Payment reserve(PaymentProvider provider, PaymentModeType modeType, OrderItem order) {
     Payment payment = execute(provider, modeType, new ReserveCommand(order));
     paymentRepository.save(payment);
     return payment;
  }

  public Payment virtualAccountReserve(
      PaymentProvider provider,
      OrderItem order,
      String bankCode
  ) {
    Payment payment = execute(
        provider,
        PaymentModeType.VIRTUAL_ACCOUNT,
        new VirtualAccountReserveCommand(order, bankCode)
    );
    paymentRepository.save(payment);
    return payment;
  }

  public Payment confirm(PaymentProvider provider, PaymentModeType modeType, String paymentId) {
    return execute(provider, modeType, new ConfirmCommand(paymentId));
  }

  public Payment confirm(String paymentId) {
    Payment payment = paymentRepository.findById(new PaymentId(paymentId));
    return confirm(
            PaymentProvider.valueOf(payment.providerId().value()),
            payment.paymentModeType(),
            paymentId
    );
  }

  public Payment confirm(String orderId, String paymentId) {
    Payment reserved = paymentRepository.findById(new PaymentId(orderId));
    Payment confirmed = confirm(
            PaymentProvider.valueOf(reserved.providerId().value()),
            reserved.paymentModeType(),
            paymentId
    );

    Map<String, String> meta = new HashMap<>(reserved.metadata());
    meta.putAll(confirmed.metadata());

    Payment merged = Payment.of(
            reserved.getId(),
            confirmed.status(),
            confirmed.paymentModeType(),
            confirmed.providerId(),
            confirmed.externalTransactionId(),
            meta
    );

    return merged;
  }

  public Payment cancel(
      PaymentProvider provider,
      PaymentModeType modeType,
      String paymentId,
      String reason
  ) {
    return execute(provider, modeType, new CancelCommand(paymentId, reason));
  }

  public Payment history(
      PaymentProvider provider,
      PaymentModeType modeType,
      String startTime,
      String endTime
  ) {
    return execute(provider, modeType, new HistoryCommand(startTime, endTime));
  }

  private Payment execute(PaymentProvider provider, PaymentModeType modeType, PaymentCommand command) {
    return paymentSupportRegistry.resolve(new ProviderId(provider.name()), modeType).execute(command);
  }
}
