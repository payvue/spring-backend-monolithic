package com.payvue.core.transaction.command;

import com.payvue.core.asset.characteristic.CapabilityManager;
import com.payvue.core.hold.HoldingsRepository;
import com.payvue.supporting.interaction.InteractionConfiguration;
import com.payvue.supporting.interaction.InteractionExecutor;
import java.util.List;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(InteractionConfiguration.class)
class TransactionConfiguration {

  @Bean
  public TransactionBindingRegistry transactionBindingRegistry(
      InteractionExecutor interactionExecutor,
      CapabilityManager capabilityManager,
      HoldingsRepository holdingsRepository) {

    return TransactionBindingRegistry.builder()
        .bind(new BindingKey(AssetType.CASH.name(), TransactionType.TRANSFER), new PolicyAwareHandlerChain(
                new CommissionPolicy(AssetType.CASH.name(), TransactionType.TRANSFER),
                new TransferCashTransactionHandler(interactionExecutor),
                List.of(new CommissionTransactionDecorator())))
        .bind(Set.of(
            new BindingKey(AssetType.COUPON.name(), TransactionType.TRANSFER),
            new BindingKey(AssetType.GIFT_CARD.name(), TransactionType.TRANSFER)
        ), new TransferTransactionHandler(interactionExecutor, holdingsRepository))
        .bind(new BindingKey(AssetType.CASH.name(), TransactionType.ACQUIRE),
            new AcquireTransactionHandler(capabilityManager, holdingsRepository))
        .bind(Set.of(
              new BindingKey(AssetType.CASH.name(), TransactionType.PAYMENT)
        ), new PolicyAwareHandlerChain(
              new CommissionPolicy(AssetType.CASH.name(), TransactionType.PAYMENT),
                new PaymentTransactionHandler(interactionExecutor),
              List.of(new CommissionTransactionDecorator())
        ))
        .bind(Set.of(
            new BindingKey(AssetType.POINT.name(), TransactionType.PAYMENT),
            new BindingKey(AssetType.GIFT_CARD.name(), TransactionType.PAYMENT),
            new BindingKey(AssetType.COUPON.name(), TransactionType.PAYMENT),
            new BindingKey(AssetType.SUBSCRIPTION.name(), TransactionType.PAYMENT)
        ), new PaymentTransactionHandler(interactionExecutor))
        .bind(Set.of(
            new BindingKey(AssetType.GAME.name(), TransactionType.TRANSFER),
            new BindingKey(AssetType.MUSIC.name(), TransactionType.TRANSFER),
            new BindingKey(AssetType.SHOPPING.name(), TransactionType.TRANSFER)
        ), new TransferTransactionHandler(interactionExecutor, holdingsRepository))
        .bind(new BindingKey(AssetType.POINT.name(), TransactionType.REWARD), new RewardTransactionHandler())
        .bind(Set.of(
            new BindingKey(AssetType.CASH.name(), TransactionType.REFUND),
            new BindingKey(AssetType.POINT.name(), TransactionType.REFUND),
            new BindingKey(AssetType.SUBSCRIPTION.name(), TransactionType.REFUND)
        ), new RefundTransactionHandler())
        .build();
  }

  @Bean
  public TransactionService transactionService(TransactionBindingRegistry registry,
                                               TransactionRepository transactionRepository) {
    return new TransactionService(registry, transactionRepository);
  }

}
