package it.bitrule.trade.task;

import dev.triumphteam.gui.guis.BaseGui;
import it.bitrule.trade.MessageAssets;
import it.bitrule.trade.component.Transaction;
import it.bitrule.trade.usecase.TradeMenuUseCase;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public final class CountdownTask extends BukkitRunnable {

    /**
     * The countdown
     */
    private final @NonNull AtomicInteger countdown;
    /**
     * This is the transaction that is being processed.
     */
    private final @NonNull Transaction transaction;
    /**
     * The player who initiated the trade.
     */
    private final @NonNull Player[] players;

    /**
     * Runs this operation.
     */
    @Override
    public void run() {
        if (this.transaction.isCancelled() || this.transaction.isEnded()) {
            this.cancel();
            return;
        }

        int remaining = this.countdown.getAndDecrement();
        int index = 0;
        for (Player player : this.players) {
            if (!player.isConnected()) {
                this.cancel();
                return;
            }

            Inventory inv = player.getOpenInventory().getTopInventory();
            if (!(inv.getHolder() instanceof BaseGui)) {
                this.cancel();
                return;
            }

            String recipientName = this.players[index == 0 ? 1 : 0].getName();
            player.sendMessage(
                    MessageAssets.ENDING_COUNTDOWN.build(
                            recipientName,
                            remaining > 1 ? remaining + " segundos" : "1 segundo"
                    )
            );

            inv.setItem(
                    12,
                    TradeMenuUseCase.getSelfReadyItemStack(recipientName, remaining)
            );

            index++;
        }

        if (remaining > 0) return;

        // TODO: Submit the end use case and after that, close the menu for each player.
    }
}