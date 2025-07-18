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
import org.jetbrains.annotations.Nullable;

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

        if (clickedInventory.getType().equals(InventoryType.PLAYER) && !clickEvent.isShiftClick()) return false;

        boolean shiftClick = clickEvent.isShiftClick() && clickedInventory.getType().equals(InventoryType.PLAYER);

        // Cancel the event if the clicked slot is not part of the viewer slots
        if (!shiftClick && Arrays.stream(Trade.VIEWER_SLOT).noneMatch(slot -> slot == clickEvent.getSlot())) return true;

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

        Inventory destinationInventory = clickEvent.getView().getTopInventory();

        // If is a shift click, we handle the item stack movement
        // and set the current item to the result of that operation.
        // Otherwise, we just let the click event proceed normally.
        if (shiftClick) {
            clickEvent.setCurrentItem(this.handleShiftClick(
                    destinationInventory,
                    clickEvent.getCurrentItem()
            ));

            clickEvent.setCancelled(true);
        }

        // This will help to compare the difference of contents before and after the click
        // to know the items that were moved or changed.
        ItemStack[] oldContents = destinationInventory.getContents();
        Bukkit.getScheduler().runTask(
                Trade.getPlugin(Trade.class),
                () -> {
                    // Synchronize the inventories of the player and the transaction
                    this.synchronize(player, transaction, oldContents, destinationInventory);

                    transaction.setClickedValue(player.getUniqueId(), false);
                }
        );

        return false;
    }

    private @Nullable ItemStack handleShiftClick(@NonNull Inventory destinationInventory, @Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return null;

        for (int viewerSlot : Trade.VIEWER_SLOT) {
            ItemStack itemStackAt = destinationInventory.getItem(viewerSlot);
            if (itemStackAt == null || itemStackAt.isEmpty()) {
                destinationInventory.setItem(viewerSlot, itemStack);
                return null; // Item has been added to an empty slot
            }

            if (!itemStackAt.isSimilar(itemStack)) continue;

            int spaceLeft = itemStackAt.getMaxStackSize() - itemStackAt.getAmount();
            if (spaceLeft <= 0) continue;

            int toAdd = Math.min(spaceLeft, itemStack.getAmount());
            itemStackAt.setAmount(itemStackAt.getAmount() + toAdd);
            itemStack.setAmount(itemStack.getAmount() - toAdd);

            destinationInventory.setItem(viewerSlot, itemStackAt);

            if (itemStack.getAmount() <= 0) return null; // All items have been added
        }

        // If we reach here, it means there are still items left to add
        return itemStack; // Return the remaining items that could not be added
    }
}