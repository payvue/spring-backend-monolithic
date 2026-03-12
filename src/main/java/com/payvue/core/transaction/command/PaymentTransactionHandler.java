package com.payvue.core.transaction.command;

import com.payvue.core.community.Community;
import com.payvue.core.community.Participant;
import com.payvue.core.hold.Holdings;
import com.payvue.supporting.interaction.ApproveDecision;
import com.payvue.supporting.interaction.Decision;
import com.payvue.supporting.interaction.ExecutePaymentIntent;
import com.payvue.supporting.interaction.Interaction;
import com.payvue.supporting.interaction.InteractionExecutor;
import lombok.RequiredArgsConstructor;

import java.util.UUID;


@RequiredArgsConstructor
final class PaymentTransactionHandler implements TransactionHandler {

  private final InteractionExecutor interactionExecutor;

  @Override
  public Transaction handle(TransactionCommand command) {

    Community buyer = command.actor().community();
    Holdings holdings = command.actor().holdings();

    UUID actor = command.actor().actorId().getValue();

    ExecutePaymentIntent intent = new ExecutePaymentIntent(
            Participant.of(actor, buyer.getParticipantRole(actor)), holdings, command.item());
    Decision<ExecutePaymentIntent> decision = new ApproveDecision<>(intent);

    Interaction interaction = interactionExecutor.executeInternal(decision, buyer);

    if (interaction.isPendingApproval()) {
      command.item().suspend();
      return Transaction.pending(command, interaction.getStatus());
    }

    if (interaction.isCompleted()) {
      return Transaction.completed(command, interaction.getStatus());
    }

    return Transaction.failed(command, interaction.getStatus());
  }
}
