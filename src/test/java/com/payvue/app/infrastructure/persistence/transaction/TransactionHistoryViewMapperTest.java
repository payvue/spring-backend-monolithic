package com.payvue.app.infrastructure.persistence.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.common.status.ExecutionState;
import com.payvue.core.transaction.command.TransactionType;
import com.payvue.core.transaction.command.meta.TransactionMeta;
import com.payvue.core.transaction.query.TransactionHistoryView;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TransactionHistoryViewMapperTest {

  @Test
  @DisplayName("TransactionEntity를 TransactionHistoryView로 매핑한다")
  void mapsToView() {
    TransactionMeta meta = TransactionMeta.fromRawMap(Map.of(
        "MERCHANT_ID", "merchant-1",
        "PAYMENT_METHOD_ID", "card",
        "PLATFORM", "web",
        "LOCATION", "seoul"
    ));
    Instant occurredAt = Instant.parse("2024-01-01T10:15:30Z");
    TransactionEntity entity = new TransactionEntity(
        "tx-1",
        TransactionType.PAYMENT,
        "CASH",
        "community-1",
        "from-community-1",
        "to-community-1",
        "item-1",
        "actor-1",
        1000L,
        "desc",
        "category",
        meta,
        ExecutionState.PENDING_APPROVAL,
        occurredAt
    );

    TransactionHistoryViewMapper mapper = new TransactionHistoryViewMapper();

    TransactionHistoryView view = mapper.toView(entity, "community-1", "payer");

    assertThat(view.core().id()).isEqualTo("tx-1");
    assertThat(view.core().kind()).isEqualTo("payment");
    assertThat(view.core().description()).isEqualTo("desc");
    assertThat(view.core().status()).isEqualTo("pending_approval");
    assertThat(view.payment().merchantId()).isEqualTo("merchant-1");
    assertThat(view.payment().paymentMethodId()).isEqualTo("card");
    assertThat(view.context().platform()).isEqualTo("web");
    assertThat(view.context().location()).isEqualTo("seoul");
    String expectedTime = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(occurredAt);
    assertThat(view.context().time()).isEqualTo(expectedTime);
  }

  @Test
  @DisplayName("수신자 관점에서는 금액이 양수이고 acquire로 표시된다")
  void mapsTransferAsAcquireForRecipient() {
    TransactionMeta meta = TransactionMeta.fromRawMap(Map.of(
        "COMMISSION", "500.00"
    ));
    TransactionEntity entity = new TransactionEntity(
        "tx-2",
        TransactionType.TRANSFER,
        "CASH",
        "community-legacy",
        "from-community",
        "to-community",
        "item-1",
        "actor-1",
        1200L,
        "desc",
        "category",
        meta,
        ExecutionState.COMPLETED,
        Instant.parse("2024-01-01T10:15:30Z")
    );

    TransactionHistoryViewMapper mapper = new TransactionHistoryViewMapper();

    TransactionHistoryView view = mapper.toView(entity, "to-community", "payer");

    assertThat(view.core().kind()).isEqualTo("acquire");
    assertThat(view.core().amount()).isEqualTo(1200L);
    assertThat(view.fee().transactionFee()).isEqualTo(500L);
  }

  @Test
  @DisplayName("송신자 관점에서는 금액이 음수로 표시된다")
  void mapsTransferAsNegativeForSender() {
    TransactionEntity entity = new TransactionEntity(
        "tx-3",
        TransactionType.TRANSFER,
        "CASH",
        "community-legacy",
        "from-community",
        "to-community",
        "item-1",
        "actor-1",
        900L,
        "desc",
        "category",
        TransactionMeta.empty(),
        ExecutionState.COMPLETED,
        Instant.parse("2024-01-01T10:15:30Z")
    );

    TransactionHistoryViewMapper mapper = new TransactionHistoryViewMapper();

    TransactionHistoryView view = mapper.toView(entity, "from-community", "payer");

    assertThat(view.core().kind()).isEqualTo("transfer");
    assertThat(view.core().amount()).isEqualTo(-900L);
  }
}
