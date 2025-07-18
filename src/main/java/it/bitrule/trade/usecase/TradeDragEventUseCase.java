package it.bitrule.trade.usecase;

import it.bitrule.trade.Trade;
import it.bitrule.trade.component.Transaction;
import it.bitrule.trade.registry.RequestsRegistry;
import it.bitrule.trade.registry.TransactionRegistry;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.logging.Logger;

public final class TradeDragEventUseCase extends SynchronizeUseCase {

    public TradeDragEventUseCase(@NonNull TransactionRegistry transactionRegistry, @NonNull RequestsRegistry requestsRegistry, @NonNull Logger logger) {
        super(transactionRegistry, requestsRegistry, logger);
    }

    public boolean submit(@NonNull Player player, @NonNull InventoryDragEvent dragEvent) {
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

        // If the drag event contains any raw slots that are not part of the trade GUI,
        // we cancel the drag event to prevent items from being moved in the trade GUI.
        // We also check if the raw slot is not part of the viewer slots.
        for (int rawSlot : dragEvent.getRawSlots()) {
            Inventory inventory = dragEvent.getView().getInventory(rawSlot);
            if (inventory == null || inventory.getType().equals(InventoryType.PLAYER)) continue;
            if (Arrays.stream(Trade.VIEWER_SLOT).anyMatch(slot -> slot == rawSlot)) continue;

            return true;
        }

        transaction.setClickedValue(player.getUniqueId(), true);

        Inventory inventory = dragEvent.getInventory();
        ItemStack[] oldContents = inventory.getContents();

        // Synchronize the inventories of the player and the transaction
        Bukkit.getScheduler().runTask(
                Trade.getPlugin(Trade.class),
                () -> {
                    // Synchronize the inventories of the player and the transaction
                    this.synchronize(player, transaction, oldContents, inventory);

                    transaction.setClickedValue(player.getUniqueId(), false);
                }
        );

        return false;
    }
}