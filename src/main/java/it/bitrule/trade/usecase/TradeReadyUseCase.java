package it.bitrule.trade.usecase;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.BaseGui;
import it.bitrule.trade.MessageAssets;
import it.bitrule.trade.Trade;
import it.bitrule.trade.component.Transaction;
import it.bitrule.trade.registry.RequestsRegistry;
import it.bitrule.trade.registry.TransactionRegistry;
import it.bitrule.trade.task.CountdownTask;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Logger;

public final class TradeReadyUseCase extends TradeUseCase {

    public final static @NonNull Function<Player, Inventory> INVENTORY_WRAPPER = p -> {
        Inventory inv = p.getOpenInventory().getTopInventory();
        if (!(inv.getHolder() instanceof BaseGui)) {
            throw new IllegalStateException("The inventory holder is not a BaseGui for player: " + p.getName());
        }
        return inv;
    };

    public TradeReadyUseCase(
            @NonNull TransactionRegistry transactionRegistry,
            @NonNull RequestsRegistry requestsRegistry,
            @NonNull Logger logger
    ) {
        super(transactionRegistry, requestsRegistry, logger);
    }

    public void submit(@NonNull Player player) {
        final Transaction transaction = this.transactionRegistry.findByPlayer(player.getUniqueId());
        if (transaction == null) {
            throw new IllegalStateException("No transaction found for player: " + player.getName());
        }

        if (transaction.isCancelled()) {
            throw new IllegalStateException("Transaction for player " + player.getName() + " has been cancelled.");
        }

        UUID recipientId;

        if (transaction.getSender().equals(player.getUniqueId())) {
            transaction.setSenderReady(!transaction.isSenderReady());
            recipientId = transaction.getReceptor();
        } else {
            transaction.setReceptorReady(!transaction.isReceptorReady());
            recipientId = transaction.getSender();
        }

        Player recipient = Bukkit.getPlayer(recipientId);
        if (recipient == null || !recipient.isOnline()) return;

        MessageAssets recipientMessageAsset;
        MessageAssets senderMessageAsset;
        if (transaction.getReadyState(player.getUniqueId())) {
            recipientMessageAsset = MessageAssets.TRANSACTION_IS_READY;
            senderMessageAsset = MessageAssets.TRANSACTION_READY;
        } else {
            recipientMessageAsset = MessageAssets.TRANSACTION_IS_NO_LONGER_READY;
            senderMessageAsset = MessageAssets.TRANSACTION_NO_LONGER_READY;
        }

        recipient.sendMessage(recipientMessageAsset.build(player.getName()));
        player.sendMessage(senderMessageAsset.build(recipient.getName()));

        boolean executorReadyState = transaction.getReadyState(player.getUniqueId());
        INVENTORY_WRAPPER.apply(player).setItem(
                12,
                getSelfReadyItemStack(recipient.getName(), executorReadyState ? 5 : 7)
        );
        INVENTORY_WRAPPER.apply(recipient).setItem(
                14,
                getOtherReadyItemStack(player.getName(), executorReadyState)
        );

        BukkitRunnable countdownTask = transaction.getBukkitRunnable();
        if (countdownTask != null && !countdownTask.isCancelled()) {
            transaction.setBukkitRunnable(null);
            countdownTask.cancel();
            return;
        }

        // If either player is not ready, we do not start the countdown
        if (!transaction.isSenderReady() || !transaction.isReceptorReady()) return;

        countdownTask = new CountdownTask(new AtomicInteger(5), transaction, new Player[]{player, recipient});
        countdownTask.runTaskTimer(JavaPlugin.getProvidingPlugin(Trade.class), 0L, 20L);
        transaction.setBukkitRunnable(countdownTask);

        for (Player p : new Player[]{player, recipient}) {
            if (!p.isConnected()) {
                countdownTask.cancel();
                return;
            }
        }
    }

    public static @NonNull ItemStack getSelfReadyItemStack(@NonNull String targetPlayerName, int remaining) {
        List<Component> lore;
        if (remaining == 7) {
            lore = MessageAssets.MENU_STATE_OPTION_LORE_SELF_NOT_DONE.buildMany();
        } else {
            lore = MessageAssets.replace(
                    MessageAssets.MENU_STATE_OPTION_LORE_SELF_DONE.buildMany(),
                    remaining > 0
                            ? MessageAssets.MENU_STATE_OPTION_LORE_SELF_DONE_COUNTDOWN.buildMany(remaining + (remaining == 1 ? " segundo" : " segundos"))
                            : MessageAssets.MENU_STATE_OPTION_LORE_SELF_DONE_WAITING.buildMany(targetPlayerName)
            );
        }

        return ItemBuilder.from(remaining == 7 ? Material.RED_CONCRETE : Material.GREEN_CONCRETE)
                .name(MessageAssets.internal("menu.state_option.display_name." + (remaining < 6 ? "self_done" : "self_not_done")))
                .lore(lore)
                .build();
    }

    public static @NonNull ItemStack getOtherReadyItemStack(@NonNull String targetPlayerName, boolean isDone) {
        MessageAssets otherDoneDisplayName = isDone
                ? MessageAssets.MENU_STATE_OPTION_DISPLAY_NAME_OTHER_DONE
                : MessageAssets.MENU_STATE_OPTION_DISPLAY_NAME_OTHER_NOT_DONE;
        MessageAssets otherDoneLore = isDone
                ? MessageAssets.MENU_STATE_OPTION_LORE_OTHER_DONE
                : MessageAssets.MENU_STATE_OPTION_LORE_OTHER_NOT_DONE;


        return ItemBuilder.from(isDone ? Material.GREEN_CONCRETE : Material.RED_CONCRETE)
                .name(otherDoneDisplayName.build(targetPlayerName).decoration(TextDecoration.ITALIC, false))
                .lore(otherDoneLore.buildMany(targetPlayerName).stream()
                        .map(line -> line.decoration(TextDecoration.ITALIC, false))
                        .collect(java.util.stream.Collectors.toList())
                )
                .build();
    }
}