package it.bitrule.trade;

import dev.triumphteam.gui.builder.item.ItemBuilder;
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
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;

public final class Trade extends JavaPlugin {

    private static final int[] GLASS_SLOT = new int[]{
            0,1,2,4,6,7,8,
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

        TradeCancelUseCase cancelUseCase = new TradeCancelUseCase(transactionRegistry, requestsRegistry);
        this.getServer().getPluginManager().registerEvents(new InventoryCloseListener(cancelUseCase), this);
        this.getServer().getPluginManager().registerEvents(new PlayerQuitListener(cancelUseCase), this);

        this.getServer().getCommandMap().register(
                "trade",
                new TradeCommand(
                        new TradeRequestUseCase(transactionRegistry, requestsRegistry),
                        new TradeAcceptUseCase(transactionRegistry, requestsRegistry)
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

        boolean isReceptorDone = transaction.getSender().equals(player.getUniqueId()) ? transaction.isReceptorDone() : transaction.isSenderDone();

        MessageAssets otherDoneDisplayName;
        if (isReceptorDone) {
            otherDoneDisplayName = MessageAssets.MENU_STATE_OPTION_DISPLAY_NAME_OTHER_DONE;
        } else {
            otherDoneDisplayName = MessageAssets.MENU_STATE_OPTION_DISPLAY_NAME_OTHER_NOT_DONE;
        }

        MessageAssets otherDoneLore;
        if (isReceptorDone) {
            otherDoneLore = MessageAssets.MENU_STATE_OPTION_LORE_OTHER_DONE;
        } else {
            otherDoneLore = MessageAssets.MENU_STATE_OPTION_LORE_OTHER_NOT_DONE;
        }

        gui.setItem(
                14,
                ItemBuilder.from(isReceptorDone ? Material.GREEN_CONCRETE : Material.RED_CONCRETE)
                        .name(otherDoneDisplayName.build(receptorName).decoration(TextDecoration.ITALIC, false))
                        .lore(otherDoneLore.buildMany(receptorName).stream()
                                .map(line -> line.decoration(TextDecoration.ITALIC, false))
                                .collect(java.util.stream.Collectors.toList())
                        )
                        .asGuiItem()
        );

        gui.setDefaultClickAction(clickEvent -> {
            if (Arrays.stream(VIEWER_SLOT).noneMatch(slot -> slot == clickEvent.getSlot())) {
                clickEvent.setCancelled(true);
                return;
            }

            // If the player who clicked is marked as done in the transaction, we cancel the click event
            boolean isSelfDone = transaction.getSender().equals(player.getUniqueId()) ? transaction.isSenderDone() : transaction.isReceptorDone();
            if (!isSelfDone) return;

            clickEvent.setCancelled(true);
        });

        gui.open(player);
    }
}