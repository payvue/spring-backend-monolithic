package com.payvue.app.application.transaction;

import com.payvue.app.application.transaction.command.TransferAssetToCommunityCommand;
import com.payvue.app.application.transaction.command.TransferAssetToUserCommand;
import com.payvue.app.application.transaction.command.TransferCashCommand;
import com.payvue.app.application.transaction.result.TransferAssetResult;
import com.payvue.app.application.transaction.result.TransferCashResult;
import com.payvue.core.transaction.command.AssetType;
import org.springframework.security.core.userdetails.UserDetails;


public interface TransactionUseCase {

  TransferCashResult transferCash(UserDetails user, TransferCashCommand command, String idempotencyKey);

  TransferAssetResult transferAssetToUser(
      UserDetails user,
      AssetType assetType,
      TransferAssetToUserCommand command,
      String idempotencyKey
  );

  TransferAssetResult transferAssetToCommunity(
      UserDetails user,
      AssetType assetType,
      TransferAssetToCommunityCommand command,
      String idempotencyKey
  );
}
