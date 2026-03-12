package com.payvue.core.transaction.query;

import com.payvue.core.transaction.command.TransactionType;
import com.payvue.shared.Period;
import org.springframework.data.domain.Pageable;

public interface TransactionQueryRepository {

  TransactionHistoryPage findPersonal(
      String actorId,
      String assetType,
      TransactionType transactionType,
      Pageable pageable,
      Period period
  );

  TransactionHistoryPage findCommunity(
      String communityId,
      String assetType,
      TransactionType transactionType,
      Pageable pageable,
      Period period
  );
}
