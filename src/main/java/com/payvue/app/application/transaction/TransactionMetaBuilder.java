package com.payvue.app.application.transaction;

import com.payvue.core.community.Community;
import com.payvue.core.transaction.command.meta.TransactionMeta;
import com.payvue.core.transaction.command.meta.TransactionMetaKey;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TransactionMetaBuilder {

  public TransactionMeta build(Community fromCommunity, String message) {
    Map<TransactionMetaKey, String> values = new HashMap<>();
    values.put(TransactionMetaKey.PLATFORM, "web");
    values.put(TransactionMetaKey.LOCATION, "payvue");
    if (fromCommunity != null && fromCommunity.getName() != null) {
      values.put(TransactionMetaKey.MERCHANT_ID, fromCommunity.getName());
    }
    if (message != null && !message.isBlank()) {
      values.put(TransactionMetaKey.MESSAGE, message);
    }
    return TransactionMeta.of(values);
  }
}
