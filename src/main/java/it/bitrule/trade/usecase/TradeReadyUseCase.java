package it.bitrule.trade.usecase;

import it.bitrule.trade.MessageAssets;
import it.bitrule.trade.Trade;
import it.bitrule.trade.component.Transaction;
import it.bitrule.trade.registry.RequestsRegistry;
import it.bitrule.trade.registry.TransactionRegistry;
import it.bitrule.trade.task.CountdownTask;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public final class TradeReadyUseCase extends TradeUseCase {

    public TradeReadyUseCase(
            @NonNull TransactionRegistry transactionRegistry,
            @NonNull RequestsRegistry requestsRegistry,
            @NonNull Logger logger
    ) {
        super(transactionRegistry, requestsRegistry, logger);
    }

    public void submit(@NonNull Player player) {
        final Transaction transaction = this.transactionRegistry.findByPlayer(player.getUniqueId());
        if (transaction == null) {
            throw new IllegalStateException("No transaction found for player: " + player.getName());
        }

        if (transaction.isCancelled()) {
            throw new IllegalStateException("Transaction for player " + player.getName() + " has been cancelled.");
        }

        UUID recipientId;
        if (transaction.getSender().equals(player.getUniqueId())) {
            transaction.setSenderReady(!transaction.isSenderReady());
            recipientId = transaction.getReceptor();
        } else {
            transaction.setReceptorReady(!transaction.isReceptorReady());
            recipientId = transaction.getSender();
        }

        Player recipient = Bukkit.getPlayer(recipientId);
        if (recipient == null || !recipient.isOnline()) return;

        MessageAssets recipientMessageAsset;
        MessageAssets senderMessageAsset;
        if (transaction.getReadyState(player.getUniqueId())) {
            recipientMessageAsset = MessageAssets.TRANSACTION_IS_READY;
            senderMessageAsset = MessageAssets.TRANSACTION_READY;
        } else {
            recipientMessageAsset = MessageAssets.TRANSACTION_IS_NO_LONGER_READY;
            senderMessageAsset = MessageAssets.TRANSACTION_NO_LONGER_READY;
        }

        recipient.sendMessage(recipientMessageAsset.build(player.getName()));
        player.sendMessage(senderMessageAsset.build(recipient.getName()));

        BukkitRunnable countdownTask = transaction.getBukkitRunnable();
        if (countdownTask != null && !countdownTask.isCancelled()) {
            transaction.setBukkitRunnable(null);
            countdownTask.cancel();
            return;
        }

        // If either player is not ready, we do not start the countdown
        if (!transaction.isSenderReady() || !transaction.isReceptorReady()) return;

        countdownTask = new CountdownTask(new AtomicInteger(5), transaction, new Player[]{player, recipient});
        countdownTask.runTaskTimer(JavaPlugin.getProvidingPlugin(Trade.class), 0L, 20L);
        transaction.setBukkitRunnable(countdownTask);

        // TODO: Update inventory to reflect the ready state to the recipient
    }
}