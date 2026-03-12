package com.payvue.app.application.transaction;

import com.payvue.core.community.CommunityId;
import com.payvue.core.transaction.command.TransactionType;
import com.payvue.shared.Period;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;

public interface TransactionQueryUseCase {

  TransactionHistoryResult queryPersonal(
      UserDetails user,
      String assetType,
      TransactionType transactionType,
      Pageable pageable,
      Period period
  );

  TransactionHistoryResult queryCommunity(
      CommunityId communityId,
      String assetType,
      TransactionType transactionType,
      Pageable pageable,
      Period period
  );
}
