package it.bitrule.trade.listener;

import it.bitrule.trade.registry.RequestsRegistry;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public final class PlayerQuitListener implements Listener {

    /**
     * Registry for managing trade requests.
     */
    private final @NonNull RequestsRegistry requestsRegistry;

    @EventHandler
    public void onPlayerQuitEvent(@NonNull final PlayerQuitEvent ev) {
        Player player = ev.getPlayer();
        this.requestsRegistry.clearReceptors(player.getUniqueId());
        this.requestsRegistry.clearSent(player.getUniqueId());
    }
}