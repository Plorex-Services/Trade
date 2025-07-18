package it.bitrule.trade;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import it.bitrule.trade.component.Transaction;
import it.bitrule.trade.manager.TradeManager;
import it.bitrule.trade.usecase.TradeReadyUseCase;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public final class Trade extends JavaPlugin {

    private static final int[] GLASS_SLOT = new int[]{
            0,1,2,3,4,5,6,7,8,
            11,13,15,
            20,21,22,23,24,
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

        TradeManager.getInstance().inject(this);
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

        gui.setItem(
                12,
                new GuiItem(
                        TradeReadyUseCase.getSelfReadyItemStack(receptorName, 7),
                        clickEvent -> {
                            if (transaction.isCancelled() || transaction.isEnded()) {
                                clickEvent.setCancelled(true);
                            } else {
                                TradeManager.getInstance().ready(player);
                            }
                        })
        );

        gui.setItem(
                14,
                new GuiItem(TradeReadyUseCase.getOtherReadyItemStack(
                        receptorName,
                        player.getUniqueId().equals(transaction.getSender()) ? transaction.isReceptorReady() : transaction.isSenderReady())
                )
        );

        gui.setDefaultClickAction(TradeManager.getInstance()::clickEvent);
        gui.setDragAction(TradeManager.getInstance()::dragEvent);

        gui.open(player);
    }
}