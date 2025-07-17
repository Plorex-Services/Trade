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

    public void submit(@NonNull Player player, @NonNull Inventory closingInventory) {
        // If the player is not trading, there's nothing to cancel.
        Transaction transaction = this.transactionRegistry.findByPlayer(player.getUniqueId());
        if (transaction == null) return;

        // If the transaction is already cancelled, there's nothing to do.
        // this helps to prevent cancelling a transaction multiple times.
        if (transaction.isCancelled()) return;

        this.transactionRegistry.unregister(transaction.getId());

        transaction.setCancelled(true);

        transaction.setReceptorDone(false);
        transaction.setSenderDone(false);

        // After marking the transaction as cancelled and resetting the done flags,
        // we need to give back the items to the players involved in the trade.
        // Then we can remove the transaction from the registry.
        this.giveBackItems(player, closingInventory);

        UUID recipientId;
        if (player.getUniqueId().equals(transaction.getSender())) {
            recipientId = transaction.getReceptor();
        } else {
            recipientId = transaction.getSender();
        }

        Player recipient = Bukkit.getPlayer(recipientId);
        if (recipient == null || !recipient.isConnected()) return;

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

        // If the recipient is online, we also give back their items.
        Inventory recipientInventory = recipient.getOpenInventory().getTopInventory();
        if (!(recipientInventory.getHolder() instanceof BaseGui)) return;

        this.giveBackItems(recipient, recipientInventory);

        if (recipientInventory.close() == 0) {
            this.logger.warning("[Receptor - Trade] Unexpected behavior... Nobody was viewing the inventory of " + recipient.getName());
        } else {
            this.logger.info("[Receptor - Trade] Closed inventory of " + recipient.getName() + " after cancelling the trade.");
        }
    }

    private void giveBackItems(@NonNull Player player, @NonNull Inventory inventory) {

        for (int slot : Trade.VIEWER_SLOT) {
            ItemStack itemStack = inventory.getItem(slot);
            if (itemStack == null || itemStack.isEmpty()) continue;

            player.getInventory()
                    .addItem(itemStack)
                    .forEach((slotIndex, dropItemStack) -> player.getWorld().dropItemNaturally(player.getLocation(), dropItemStack));
        }
    }
}