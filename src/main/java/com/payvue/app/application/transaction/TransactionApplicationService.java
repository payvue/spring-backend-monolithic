package com.payvue.app.application.transaction;

import com.payvue.generic.alarm.AlarmContextManager;

import com.payvue.app.application.transaction.transfer.TransferAssetToCommunityFlow;
import com.payvue.app.application.transaction.transfer.TransferAssetToUserFlow;
import com.payvue.app.application.transaction.transfer.TransferCashFlow;
import com.payvue.app.application.transaction.command.TransferAssetToCommunityCommand;
import com.payvue.app.application.transaction.command.TransferAssetToUserCommand;
import com.payvue.app.application.transaction.command.TransferCashCommand;
import com.payvue.app.application.transaction.result.TransferAssetResult;
import com.payvue.app.application.transaction.result.TransferCashResult;
import com.payvue.app.domain.asset.OnlineAssetService;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.core.community.CommunityRepository;
import com.payvue.core.hold.HoldingsRepository;
import com.payvue.core.transaction.command.Transaction;
import com.payvue.core.transaction.command.TransactionRepository;
import com.payvue.core.transaction.command.TransactionService;
import com.payvue.supporting.interaction.InteractionExecutor;
import com.payvue.supporting.user.UserContextManager;

import com.payvue.core.transaction.command.AssetType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionApplicationService implements TransactionUseCase {

  private final TransactionService transactionService;
  private final CommunityContextManager communityContextManager;
  private final HoldingsRepository holdingsRepository;
  private final TransactionRepository transactionRepository;
  private final CommunityRepository communityRepository;
  private final TransactionMetaBuilder transactionMetaBuilder;

  private final OnlineAssetService onlineAssetService;
  private final InteractionExecutor interactionExecutor;
  private final AlarmContextManager alarmChannelRouter;
  private final UserContextManager userContextManager;

  @Override
  @Transactional
  public TransferCashResult transferCash(
      UserDetails user,
      TransferCashCommand command,
      String idempotencyKey
  ) {
    var context = TransferCashFlow.start(command, idempotencyKey)
        .transaction(step -> step.checkIdempotency(transactionRepository))
        .user(step -> step.loadSender(userContextManager, user.getUsername()))
        .community(step -> step.loadPersonalCommunity(communityContextManager)
                .loadRecipientCommunity(userContextManager, communityContextManager)
        )
        .holdings(step -> step.loadHoldings(holdingsRepository).loadRecipientHoldings(holdingsRepository))
        .asset(step -> step.loadCashItem(onlineAssetService))
        .interaction(step -> step.executeInteraction(interactionExecutor))
        .transaction(step -> step.process(
            transactionService,
            transactionRepository,
            transactionMetaBuilder
        ))
        .holdings(step -> step.saveHoldings(holdingsRepository))
        .notify(step -> step.sendAlarm(alarmChannelRouter))
        .end();

    Transaction transaction = context.transaction();
    return new TransferCashResult(
        transaction.getId().toString(),
        transaction.interactionState().name().toLowerCase(),
        command.amount()
    );
  }

  @Override
  @Transactional
  public TransferAssetResult transferAssetToUser(
      UserDetails user,
      AssetType assetType,
      TransferAssetToUserCommand command,
      String idempotencyKey
  ) {
    var context = TransferAssetToUserFlow.start(command, assetType)
        .user(step -> step.loadSender(userContextManager, user.getUsername())
                .loadRecipient(userContextManager))
        .community(step -> step.loadPersonalCommunities(communityContextManager))
        .holdings(step -> step.loadHoldings(holdingsRepository))
        .asset(step -> step.resolveAssetItem(onlineAssetService))
        .transaction(step -> step.process(
            transactionService,
            transactionRepository,
            transactionMetaBuilder
        ))
        .notify(step -> step.sendAlarm(alarmChannelRouter))
        .end();

    Transaction transaction = context.transaction();
    return new TransferAssetResult(
        transaction.getId().toString(),
        transaction.interactionState().name().toLowerCase()
    );
  }

  @Override
  @Transactional
  public TransferAssetResult transferAssetToCommunity(
      UserDetails user,
      AssetType assetType,
      TransferAssetToCommunityCommand command,
      String idempotencyKey
  ) {
    var context = TransferAssetToCommunityFlow.start(command, assetType)
        .user(step -> step.loadSender(userContextManager, user.getUsername()))
        .community(step -> step.loadCommunities(communityContextManager, communityRepository))
        .holdings(step -> step.loadHoldings(holdingsRepository))
        .asset(TransferAssetToCommunityFlow::resolveAssetItem)
        .transaction(step -> step.process(
            transactionService,
            transactionRepository,
            transactionMetaBuilder
        ))
        .notify(step -> step.sendAlarm(alarmChannelRouter))
        .end();

    Transaction transaction = context.transaction();
    return new TransferAssetResult(
        transaction.getId().toString(),
        transaction.interactionState().name().toLowerCase()
    );
  }

}
