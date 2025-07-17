package it.bitrule.trade.listener;

import dev.triumphteam.gui.guis.BaseGui;
import it.bitrule.trade.usecase.TradeCancelUseCase;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.Optional;

/**
 * Why not use the handler from the {@link dev.triumphteam.gui.guis.Gui}?
 * Because I don't want to keep a static field of the cancelUseCase, so I handle the inventory close event
 * to cancel trades when a player closes their inventory.
 */
@RequiredArgsConstructor
public final class InventoryCloseListener implements Listener {

    /**
     * This use case is responsible for handling the cancellation of trades
     * when a player closes their inventory.
     */
    private final @NonNull TradeCancelUseCase cancelUseCase;

    @EventHandler
    public void onInventoryCloseEvent(@NonNull final InventoryCloseEvent ev) {
        if (!(ev.getInventory().getHolder() instanceof BaseGui)) return;

        Player player = Optional.of(ev.getPlayer())
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .orElse(null);
        if (player == null) {
            throw new IllegalStateException("InventoryCloseEvent was triggered by a non-player entity.");
        }

        // Submit the trade cancellation request for the player.
        this.cancelUseCase.submit(player, ev.getInventory());
    }
}