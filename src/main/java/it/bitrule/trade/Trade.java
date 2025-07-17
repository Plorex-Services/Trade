package it.bitrule.trade;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.Gui;
import it.bitrule.trade.command.TradeCommand;
import it.bitrule.trade.component.Transaction;
import it.bitrule.trade.listener.InventoryCloseListener;
import it.bitrule.trade.listener.PlayerQuitListener;
import it.bitrule.trade.registry.RequestsRegistry;
import it.bitrule.trade.registry.TransactionRegistry;
import it.bitrule.trade.usecase.TradeAcceptUseCase;
import it.bitrule.trade.usecase.TradeCancelUseCase;
import it.bitrule.trade.usecase.TradeRequestUseCase;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Trade extends JavaPlugin {

    private static final int[] GLASS_SLOT = new int[]{
            0,1,2,3,4,5,6,7,8,
            11,13,15,
            20,21,23,24,
            30,31,32,
            40,
            49
    };

    public static final int[] VIEWER_SLOT = new int[]{
            9,10,
            18,19,
            27,28,29,
            36,37,38,39,
            45,46,47,48
    };

    public void onEnable() {
        this.saveResource("messages.yml", true);

        MessageAssets.adjustInternal(YamlConfiguration.loadConfiguration(new File(this.getDataFolder(), "messages.yml")));

        TransactionRegistry transactionRegistry = new TransactionRegistry();
        RequestsRegistry requestsRegistry = new RequestsRegistry();

        TradeCancelUseCase cancelUseCase = new TradeCancelUseCase(transactionRegistry, requestsRegistry, this.getLogger());
        this.getServer().getPluginManager().registerEvents(new InventoryCloseListener(cancelUseCase), this);
        this.getServer().getPluginManager().registerEvents(new PlayerQuitListener(cancelUseCase), this);

        this.getServer().getCommandMap().register(
                "trade",
                new TradeCommand(
                        new TradeRequestUseCase(transactionRegistry, requestsRegistry, this.getLogger()),
                        new TradeAcceptUseCase(transactionRegistry, requestsRegistry, this.getLogger())
                )
        );
    }

    public static void showGui(@NonNull Player player, @NonNull Transaction transaction, @NonNull String receptorName) {
        Gui gui = Gui.gui()
                .rows(6)
                .disableItemDrop()
                .title(MessageAssets.MENU_TITLE.build(receptorName))
                .create();

        gui.setItem(
                Arrays.stream(GLASS_SLOT).boxed().toList(),
                ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE)
                        .name(Component.empty())
                        .lore(Component.empty())
                        .asGuiItem()
        );

        boolean isSelfSender = player.getUniqueId().equals(transaction.getSender());
        boolean isRecipientDone = isSelfSender ? transaction.isReceptorDone() : transaction.isSenderDone();

        MessageAssets otherDoneDisplayName;
        if (isRecipientDone) {
            otherDoneDisplayName = MessageAssets.MENU_STATE_OPTION_DISPLAY_NAME_OTHER_DONE;
        } else {
            otherDoneDisplayName = MessageAssets.MENU_STATE_OPTION_DISPLAY_NAME_OTHER_NOT_DONE;
        }

        MessageAssets otherDoneLore;
        if (isRecipientDone) {
            otherDoneLore = MessageAssets.MENU_STATE_OPTION_LORE_OTHER_DONE;
        } else {
            otherDoneLore = MessageAssets.MENU_STATE_OPTION_LORE_OTHER_NOT_DONE;
        }

        gui.setItem(
                14,
                ItemBuilder.from(isRecipientDone ? Material.GREEN_CONCRETE : Material.RED_CONCRETE)
                        .name(otherDoneDisplayName.build(receptorName).decoration(TextDecoration.ITALIC, false))
                        .lore(otherDoneLore.buildMany(receptorName).stream()
                                .map(line -> line.decoration(TextDecoration.ITALIC, false))
                                .collect(java.util.stream.Collectors.toList())
                        )
                        .asGuiItem()
        );

        AtomicBoolean clickQueue = new AtomicBoolean(false);
        gui.setDragAction(dragEvent -> {
            Inventory inventory = dragEvent.getInventory();
            // If the player who clicked is marked as done in the transaction, we cancel the drag event
            if (isSelfSender ? transaction.isSenderDone() : transaction.isReceptorDone()) {
                dragEvent.setCancelled(true);
                return;
            }

            if (clickQueue.get() || transaction.isEnded() || transaction.isCancelled()) {
                dragEvent.setCancelled(true);
                return;
            }

            clickQueue.set(true);

            // Synchronize the inventories of the player and the transaction
            Bukkit.getScheduler().runTask(
                    Trade.getPlugin(Trade.class),
                    () -> {
                        // Synchronize the inventories of the player and the transaction
                        synchronizeInventories(player, transaction, inventory);

                        clickQueue.set(false);
                    }
            );
        });

        gui.setDefaultClickAction(clickEvent -> {
            Inventory clickedInventory = clickEvent.getClickedInventory();
            if (clickedInventory == null) {
                throw new IllegalStateException("Clicked inventory is null");
            }

            if (clickedInventory.getType().equals(InventoryType.PLAYER)) return;

            if (Arrays.stream(VIEWER_SLOT).noneMatch(slot -> slot == clickEvent.getSlot())) {
                clickEvent.setCancelled(true);
                return;
            }

            // If the player who clicked is marked as done in the transaction, we cancel the click event
            if (isSelfSender ? transaction.isSenderDone() : transaction.isReceptorDone()) {
                clickEvent.setCancelled(true);
                return;
            }

            if (clickQueue.get() || transaction.isEnded() || transaction.isCancelled()) {
                clickEvent.setCancelled(true);
                return;
            }

            clickQueue.set(true);
            Bukkit.getScheduler().runTask(
                    Trade.getPlugin(Trade.class),
                    () -> {
                        // Synchronize the inventories of the player and the transaction
                        synchronizeInventories(player, transaction, clickedInventory);
                        clickQueue.set(false);
                    }
            );
        });

        gui.open(player);
    }

    private static void synchronizeInventories(@NonNull Player player, @NonNull Transaction transaction, @NonNull Inventory from) {
        UUID recipientId;
        if (transaction.getSender().equals(player.getUniqueId())) {
            recipientId = transaction.getReceptor();
        } else {
            recipientId = transaction.getSender();
        }

        Player recipient = Bukkit.getPlayer(recipientId);
        if (recipient == null || !recipient.isConnected()) {
            player.closeInventory();
            return;
        }

        // If the recipient is not viewing a trade GUI, we cancel the click event
        Inventory recipientInventory = recipient.getOpenInventory().getTopInventory();
        if (!(recipientInventory.getHolder() instanceof BaseGui)) {
            player.closeInventory();
            return;
        }

        for (int slot : VIEWER_SLOT) {
            ItemStack itemStack = from.getItem(slot);
            if (itemStack == null || itemStack.isEmpty()) itemStack = new ItemStack(Material.AIR);

            recipientInventory.setItem(slotToViewer(slot), itemStack);
        }
    }

    public static int slotToViewer(int slot) {
        if (slot <= 19) return slot + 7;
        if (slot <= 29) return slot + 6;

        return slot + 5;
    }
}