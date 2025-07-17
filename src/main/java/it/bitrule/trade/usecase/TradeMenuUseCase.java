package it.bitrule.trade.usecase;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import it.bitrule.trade.MessageAssets;
import it.bitrule.trade.registry.RequestsRegistry;
import it.bitrule.trade.registry.TransactionRegistry;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.logging.Logger;

public final class TradeMenuUseCase extends SynchronizeUseCase {

    public TradeMenuUseCase(
            @NonNull TransactionRegistry transactionRegistry,
            @NonNull RequestsRegistry requestsRegistry,
            @NonNull Logger logger
    ) {
        super(transactionRegistry, requestsRegistry, logger);
    }

    public static @NonNull ItemStack getSelfReadyItemStack(@NonNull String targetPlayerName, int remaining) {
        List<Component> lore;
        if (remaining < 6) {
            lore = MessageAssets.replace(
                    MessageAssets.MENU_STATE_OPTION_LORE_SELF_DONE.buildMany(),
                    remaining > 0
                            ? MessageAssets.MENU_STATE_OPTION_LORE_SELF_DONE_COUNTDOWN.buildMany(remaining + (remaining == 1 ? " segundo" : " segundos"))
                            : MessageAssets.MENU_STATE_OPTION_LORE_SELF_DONE_WAITING.buildMany(targetPlayerName)
            );
        } else {
            lore = MessageAssets.MENU_STATE_OPTION_LORE_SELF_NOT_DONE.buildMany();
        }

        return ItemBuilder.from(remaining == 6 ? Material.RED_CONCRETE : Material.GREEN_CONCRETE)
                .name(MessageAssets.internal("menu.state_option.display_name." + (remaining < 6 ? "self_done" : "self_not_done")))
                .lore(lore)
                .build();
    }
}