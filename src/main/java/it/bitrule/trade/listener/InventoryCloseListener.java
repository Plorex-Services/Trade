package it.bitrule.trade.listener;

import dev.triumphteam.gui.guis.BaseGui;
import it.bitrule.trade.manager.TradeManager;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.Optional;

public final class InventoryCloseListener implements Listener {

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

        TradeManager.getInstance().cancel(player, ev.getInventory());
    }
}