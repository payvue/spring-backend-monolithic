package com.payvue.core.transaction.command;

public class TransactionService {

  private final TransactionBindingRegistry registry;
  private final TransactionRepository transactionRepository;

  public TransactionService(TransactionBindingRegistry registry, TransactionRepository transactionRepository) {
    this.registry = registry;
      this.transactionRepository = transactionRepository;
  }

  public Transaction process(TransactionCommand command) {

    TransactionBinding binding = registry.resolve(resolveCategoryName(command.assetType()), command.type());
    Actor ctx = command.actor();
    if (command.actor().binderKey() == null) {
      ctx = new Actor(
          command.actor().community(),
          command.actor().holdings(),
          command.actor().actorId(),
          binding.key()
      );
    }

    Transaction transaction =  binding.handler().handle(new TransactionCommand(
        command.type(),
        command.assetType(),
        command.item(),
        command.amount(),
        ctx,
        command.meta(),
        command.from(),
        command.to()
    ));

    transactionRepository.save(transaction);
    return transaction;
  }

  private String resolveCategoryName(String assetType) {
    if (assetType == null) {
      return null;
    }
    String normalized = assetType.toUpperCase(java.util.Locale.ROOT);
    return switch (normalized) {
      case "GIFT_CARD" -> "GIFT";
      case "POINT" -> "MILEAGE";
      default -> normalized;
    };
  }
}
