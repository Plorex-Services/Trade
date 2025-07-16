package it.bitrule.trade.registry;

import it.bitrule.trade.component.Transaction;
import lombok.NonNull;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TransactionRegistry {

    /**
     * This map stores the transaction ID associated with each player.
     */
    private final @NonNls Map<UUID, UUID> transactionIdByPlayer = new ConcurrentHashMap<>();
    /**
     * This map stores the transactions by their unique ID.
     */
    private final @NonNull Map<UUID, Transaction> transactions = new ConcurrentHashMap<>();

    /**
     * Registers a new transaction.
     *
     * @param playerId the unique ID of the player
     * @return the transaction that was registered
     */
    public @Nullable Transaction findByPlayer(@NonNull UUID playerId) {
        return Optional.ofNullable(this.transactionIdByPlayer.get(playerId))
                .map(this::findById)
                .orElse(null);
    }

    /**
     * Finds a transaction by its unique ID.
     *
     * @param transactionId the unique ID of the transaction
     * @return the transaction if found, or null if not found
     */
    public @Nullable Transaction findById(@NonNull UUID transactionId) {
        return this.transactions.get(transactionId);
    }

    /**
     * Registers a new transaction in the registry.
     *
     * @param transaction the transaction to registers
     */
    public void register(@NonNull Transaction transaction) {
        this.transactions.put(transaction.getId(), transaction);

        // Register the transaction ID for both the sender and the receptor
        this.transactionIdByPlayer.put(transaction.getReceptor(), transaction.getId());
        this.transactionIdByPlayer.put(transaction.getSender(), transaction.getId());
    }

    /**
     * Unregisters a transaction from the registry.
     *
     * @param transactionId the unique ID of the transaction to unregister
     */
    public void unregister(@NonNull UUID transactionId) {
        Transaction transaction = this.transactions.remove(transactionId);
        if (transaction == null) return;

        // Remove the transaction ID for both the sender and the receptor
        this.transactionIdByPlayer.remove(transaction.getReceptor());
        this.transactionIdByPlayer.remove(transaction.getSender());
    }
}