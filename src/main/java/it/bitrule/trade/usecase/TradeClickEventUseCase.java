package it.bitrule.trade.usecase;

import it.bitrule.trade.Trade;
import it.bitrule.trade.component.Transaction;
import it.bitrule.trade.registry.RequestsRegistry;
import it.bitrule.trade.registry.TransactionRegistry;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.logging.Logger;

public final class TradeClickEventUseCase extends SynchronizeUseCase {

    public TradeClickEventUseCase(
            @NonNull TransactionRegistry transactionRegistry,
            @NonNull RequestsRegistry requestsRegistry,
            @NonNull Logger logger
    ) {
        super(transactionRegistry, requestsRegistry, logger);
    }

    public boolean submit(@NonNull Player player, @NonNull InventoryClickEvent clickEvent) {
        Inventory clickedInventory = clickEvent.getClickedInventory();
        if (clickedInventory == null) {
            throw new IllegalStateException("Trying to handle a click outside of an inventory.");
        }

        if (clickedInventory.getType().equals(InventoryType.PLAYER)) return false;

        // Cancel the event if the clicked slot is not part of the viewer slots
        if (Arrays.stream(Trade.VIEWER_SLOT).noneMatch(slot -> slot == clickEvent.getSlot())) return true;

        Transaction transaction = this.transactionRegistry.findByPlayer(player.getUniqueId());
        if (transaction == null) {
            throw new IllegalStateException("No transaction found for player: " + player.getName());
        }

        if (transaction.isCancelled()) {
            throw new IllegalStateException("Transaction for player " + player.getName() + " has been cancelled.");
        }

        if (transaction.getClickedValue(player.getUniqueId())) return true;
        if (transaction.getReadyState(player.getUniqueId())) return true;

        if (transaction.isEnded()) return true;

        transaction.setClickedValue(player.getUniqueId(), true);

        // This will help to compare the difference of contents before and after the click
        // to know the items that were moved or changed.
        ItemStack[] oldContents = clickedInventory.getContents();
        Bukkit.getScheduler().runTask(
                Trade.getPlugin(Trade.class),
                () -> {
                    // Synchronize the inventories of the player and the transaction
                    this.synchronize(player, transaction, oldContents, clickedInventory);

                    transaction.setClickedValue(player.getUniqueId(), false);
                }
        );

        return false;
    }
}