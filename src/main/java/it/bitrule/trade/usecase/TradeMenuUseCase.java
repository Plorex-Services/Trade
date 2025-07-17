package it.bitrule.trade.usecase;

import dev.triumphteam.gui.guis.BaseGui;
import it.bitrule.trade.Trade;
import it.bitrule.trade.component.ChangedItemStack;
import it.bitrule.trade.component.Transaction;
import it.bitrule.trade.registry.RequestsRegistry;
import it.bitrule.trade.registry.TransactionRegistry;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Logger;

public final class TradeMenuUseCase extends TradeUseCase {

    public TradeMenuUseCase(
            @NonNull TransactionRegistry transactionRegistry,
            @NonNull RequestsRegistry requestsRegistry,
            @NonNull Logger logger
    ) {
        super(transactionRegistry, requestsRegistry, logger);
    }

    public void executeClickEvent(@NonNull Player player, @NonNull Transaction transaction, @NonNull InventoryClickEvent clickEvent) {
        Inventory clickedInventory = clickEvent.getClickedInventory();
        if (clickedInventory == null) {
            throw new IllegalStateException("Clicked inventory is null");
        }

        if (clickedInventory.getType().equals(InventoryType.PLAYER)) return;

        if (Arrays.stream(Trade.VIEWER_SLOT).noneMatch(slot -> slot == clickEvent.getSlot())) {
            clickEvent.setCancelled(true);
            return;
        }

        // If the player who clicked is marked as done in the transaction, we cancel the click event
        if (transaction.getReadyState(player.getUniqueId())) {
            clickEvent.setCancelled(true);
            return;
        }

        if (transaction.getClickedValue(player.getUniqueId()) || transaction.isEnded() || transaction.isCancelled()) {
            clickEvent.setCancelled(true);
            return;
        }

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
    }

    public void executeDragEvent(@NonNull Player player, @NonNull Transaction transaction, @NonNull InventoryDragEvent dragEvent) {
        // If the player who clicked is marked as done in the transaction, we cancel the drag event
        if (transaction.getReadyState(player.getUniqueId())) {
            dragEvent.setCancelled(true);
            return;
        }

        if (transaction.getClickedValue(player.getUniqueId()) || transaction.isEnded() || transaction.isCancelled()) {
            dragEvent.setCancelled(true);
            return;
        }

        // If the drag event contains any raw slots that are not part of the trade GUI,
        // we cancel the drag event to prevent items from being moved in the trade GUI.
        // We also check if the raw slot is not part of the viewer slots.
        for (int rawSlot : dragEvent.getRawSlots()) {
            Inventory inventory = dragEvent.getView().getInventory(rawSlot);
            if (inventory == null || inventory.getType().equals(InventoryType.PLAYER)) continue;
            if (Arrays.stream(Trade.VIEWER_SLOT).anyMatch(slot -> slot == rawSlot)) continue;

            dragEvent.setCancelled(true);

            return;
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
    }

    /**
     * Synchronizes the inventories of the player and the transaction.
     * This method is called when the player clicks on the trade GUI.
     * It updates the items in the trade GUI and the transaction
     * based on the player's actions.
     * @param player the player who clicked on the trade GUI
     * @param transaction the transaction associated with the trade
     * @param oldContents the contents of the inventory before the click. It is annotated as @Nullable just for the Class Type, then the array is never null.
     * @param from the inventory from which the click originated
     */
    private void synchronize(
            @NonNull Player player,
            @NonNull Transaction transaction,
            @Nullable ItemStack @NonNull [] oldContents,
            @NonNull Inventory from
    ) {
        UUID recipientId;
        if (transaction.getSender().equals(player.getUniqueId())) {
            recipientId = transaction.getReceptor();
        } else {
            recipientId = transaction.getSender();
        }

        // If the recipient is not viewing a trade GUI, we cancel the click event
        Inventory recipientInventory = Optional.ofNullable(Bukkit.getPlayer(recipientId))
                .filter(Player::isConnected)
                .map(Player::getOpenInventory)
                .map(org.bukkit.inventory.InventoryView::getTopInventory)
                .orElse(null);
        if (recipientInventory == null || !(recipientInventory.getHolder() instanceof BaseGui)) {
            player.closeInventory();
            return;
        }

        for (int slot : Trade.VIEWER_SLOT) {
            ItemStack itemStack = from.getItem(slot);
            if (itemStack == null || itemStack.isEmpty()) itemStack = new ItemStack(Material.AIR);

            recipientInventory.setItem(this.slotToViewer(slot), itemStack);
        }

        // This set will hold the items that were changed during the synchronization.
        // It will be used to determine what items were added, removed, or changed
        // in the trade GUI after the player clicked on it.
        // After comparing the contents of the inventories and knowing what items were changed,
        // we're going to call an event to log at database the changes made at the transaction.
        Set<ChangedItemStack> changedItems = new HashSet<>();

        // After synchronizing the inventories, we're going to compare the contents
        // to see if there was any change and where it was made.
        ItemStack[] newContents = from.getContents();
        for (int i = 0; i < newContents.length; i++) {
            ItemStack newItem = newContents[i];
            ItemStack oldItem = oldContents[i];

            if (newItem == null && oldItem == null) continue;

            // If oldItem is null, it means the item was added.
            if (oldItem == null) {
                changedItems.add(new ChangedItemStack(
                        i,
                        null,
                        newItem,
                        ChangedItemStack.ChangeType.ADD
                ));
            } else if (newItem == null) { // If newItem is null, it means the item was removed.
                changedItems.add(new ChangedItemStack(
                        i,
                        oldItem,
                        null,
                        ChangedItemStack.ChangeType.REMOVE
                ));
            } else if (!oldItem.equals(newItem)) { // If both items are not null, we check if they are different.
                changedItems.add(new ChangedItemStack(
                        i,
                        oldItem,
                        newItem,
                        ChangedItemStack.ChangeType.CHANGE
                ));
            }
        }

        if (changedItems.isEmpty()) {
            // If there are no changes, we return early.
            return;
        }

        this.logger.info("Synchronizing trade GUI for player " + player.getName() + " in transaction " + transaction.getId());
        this.logger.info("Changed items: " + changedItems);
    }

    private int slotToViewer(int slot) {
        if (slot <= 19) return slot + 7;
        if (slot <= 29) return slot + 6;

        return slot + 5;
    }
}