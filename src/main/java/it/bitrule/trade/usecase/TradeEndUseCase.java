package it.bitrule.trade.usecase;

import it.bitrule.trade.MessageAssets;
import it.bitrule.trade.component.Transaction;
import it.bitrule.trade.registry.RequestsRegistry;
import it.bitrule.trade.registry.TransactionRegistry;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;
import java.util.logging.Logger;

public final class TradeEndUseCase extends TradeUseCase {

    public TradeEndUseCase(
            @NonNull TransactionRegistry transactionRegistry,
            @NonNull RequestsRegistry requestsRegistry,
            @NonNull Logger logger
    ) {
        super(transactionRegistry, requestsRegistry, logger);
    }

    public void submit(@NonNull Player[] participants, @NonNull UUID transactionId) {
        final Transaction transaction = this.transactionRegistry.findById(transactionId);
        if (transaction == null) {
            throw new IllegalStateException("No transaction found with ID: " + transactionId);
        }

        if (transaction.isCancelled()) {
            throw new IllegalStateException("Transaction with ID " + transactionId + " has been cancelled.");
        }

        if (transaction.isEnded()) {
            throw new IllegalStateException("Transaction with ID " + transactionId + " has already ended.");
        }

        Player firstParticipant = participants[0];
        if (!firstParticipant.isConnected()) {
            throw new IllegalStateException("Participant " + firstParticipant.getName() + " is not connected.");
        }

        Player secondParticipant = participants[1];
        if (!secondParticipant.isConnected()) {
            throw new IllegalStateException("Participant " + secondParticipant.getName() + " is not connected.");
        }

        firstParticipant.sendMessage(MessageAssets.TRANSACTION_ENDED.build(
                secondParticipant.getName()
        ));
        secondParticipant.sendMessage(MessageAssets.TRANSACTION_ENDED.build(
                firstParticipant.getName()
        ));

        Inventory firstInventory = TradeReadyUseCase.INVENTORY_WRAPPER.apply(firstParticipant);
        Inventory secondInventory = TradeReadyUseCase.INVENTORY_WRAPPER.apply(secondParticipant);

        TradeCancelUseCase.giveBackItems(firstParticipant, secondInventory);
        TradeCancelUseCase.giveBackItems(secondParticipant, firstInventory);

        this.transactionRegistry.unregister(transactionId);
        transaction.setEnded(true);

        if (firstInventory.close() == 0) {
            this.logger.warning("Failed to close inventory for player: " + firstParticipant.getName());
        }

        if (secondInventory.close() == 0) {
            this.logger.warning("Failed to close inventory for player: " + secondParticipant.getName());
        }
    }
}