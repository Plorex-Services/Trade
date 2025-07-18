package it.bitrule.trade.usecase;

import dev.triumphteam.gui.guis.BaseGui;
import it.bitrule.trade.MessageAssets;
import it.bitrule.trade.Trade;
import it.bitrule.trade.component.Transaction;
import it.bitrule.trade.registry.RequestsRegistry;
import it.bitrule.trade.registry.TransactionRegistry;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;
import java.util.logging.Logger;

public final class TradeCancelUseCase extends TradeUseCase {

    public TradeCancelUseCase(
            @NonNull TransactionRegistry transactionRegistry,
            @NonNull RequestsRegistry requestsRegistry,
            @NonNull Logger logger
    ) {
        super(transactionRegistry, requestsRegistry, logger);
    }

    /**
     * Cancels an ongoing trade transaction.
     * This method is used to cancel a trade transaction when a player decides to close the inventory or leave from the server.
     * It checks if the player is currently trading, if the transaction is already ended or cancelled,
     * and if not, it proceeds to cancel the transaction.
     * It resets the done flags for both players involved in the trade,
     * gives back the items to the players involved in the trade,
     * closes the inventory of the recipient player,
     * and sends messages to both players involved in the trade to notify them that the trade has been cancelled.
     * @param player the player who is cancelling the trade
     * @param closingInventory the inventory that is being closed, usually the player's inventory
     */
    public void submit(@NonNull Player player, @NonNull Inventory closingInventory) {
        // If the player is not trading, there's nothing to cancel.
        Transaction transaction = this.transactionRegistry.findByPlayer(player.getUniqueId());
        if (transaction == null) return;

        // If the transaction is already ended, there's nothing to do.
        if (transaction.isEnded()) return;

        // If the transaction is already cancelled, there's nothing to do.
        // this helps to prevent cancelling a transaction multiple times.
        if (transaction.isCancelled()) return;

        this.transactionRegistry.unregister(transaction.getId());

        transaction.setCancelled(true);

        transaction.setReceptorReady(false);
        transaction.setSenderReady(false);

        // After marking the transaction as cancelled and resetting the done flags,
        // we need to give back the items to the players involved in the trade.
        // Then we can remove the transaction from the registry.
        this.giveBackItems(player, closingInventory);

        // Search the id of the other player involved in the trade.
        // If the player is the sender, the recipient is the receptor, and vice versa.
        UUID recipientId;
        if (player.getUniqueId().equals(transaction.getSender())) {
            recipientId = transaction.getReceptor();
        } else {
            recipientId = transaction.getSender();
        }


        BukkitRunnable bukkitRunnable = transaction.getBukkitRunnable();
        boolean cancelledRunnable = bukkitRunnable != null && !bukkitRunnable.isCancelled();
        if (cancelledRunnable) bukkitRunnable.cancel();

        Player recipient = Bukkit.getPlayer(recipientId);
        if (recipient == null || !recipient.isConnected()) return;

        if (cancelledRunnable)
            this.logger.warning("[Receptor - Trade] Cancelled the countdown for the trade between " + player.getName() + " and " + recipient.getName());

        player.sendMessage(
                MessageAssets.TRANSACTION_CANCELLED.build(
                        recipient.getName()
                )
        );

        recipient.sendMessage(
                MessageAssets.TRANSACTION_WAS_CANCELLED.build(
                        player.getName()
                )
        );

        // If the recipient is viewing a trade GUI, we need to close it.
        // but first we need to give back the items to the recipient.
        Inventory recipientInventory = recipient.getOpenInventory().getTopInventory();
        if (!(recipientInventory.getHolder() instanceof BaseGui)) return;

        giveBackItems(recipient, recipientInventory);

        if (recipientInventory.close() == 0) {
            this.logger.warning("[Receptor - Trade] Unexpected behavior... Nobody was viewing the inventory of " + recipient.getName());
        } else {
            this.logger.info("[Receptor - Trade] Closed inventory of " + recipient.getName() + " after cancelling the trade.");
        }
    }

    public static void giveBackItems(@NonNull Player player, @NonNull Inventory inventory) {
        for (int slot : Trade.VIEWER_SLOT) {
            ItemStack itemStack = inventory.getItem(slot);
            if (itemStack == null || itemStack.isEmpty()) continue;

            player.getInventory()
                    .addItem(itemStack)
                    .forEach((slotIndex, dropItemStack) -> player.getWorld().dropItemNaturally(player.getLocation(), dropItemStack));
        }
    }
}