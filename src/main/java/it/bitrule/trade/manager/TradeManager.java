package it.bitrule.trade.manager;

import it.bitrule.trade.Trade;
import it.bitrule.trade.usecase.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

@RequiredArgsConstructor
public final class TradeManager {

    @Getter private final static @NonNull TradeManager instance = TradeManagerFactory.create();
    /**
     * This is the use case that handles the logic when a
     * player requests a trade with another player.
     * It will check if the recipient is online,
     * if the recipient is already in a trade,
     * and if the recipient has any active trade requests.
     * If all checks pass, it will send a trade request
     * to the recipient player.
     */
    private final @NonNull TradeRequestUseCase requestUseCase;
    /**
     * This is the use case that handles the logic when a
     * player accepts a trade request.
     * It will check if the player is already in a trade,
     * if the player has any active trade requests,
     * and if the recipient is online.
     * If all checks pass, it will
     * mark the trade as accepted
     * and update the trade GUI for both players.
     */
    private final @NonNull TradeAcceptUseCase acceptUseCase;
    /**
     * This is the use case that handles the logic when a
     * player is ready to complete the trade. If both players
     * are ready, it will start a countdown to complete the trade.
     */
    private final @NonNull TradeReadyUseCase readyUseCase;
    /**
     * This is the use case that handles the logic when a
     * transaction needs to be cancelled. Like when a player
     * closes the inventory or leaves the server
     */
    private final @NonNull TradeCancelUseCase cancelUseCase;
    /**
     * This is the use case that handles the drag event
     * in the trade GUI.
     */
    private final @NonNull TradeDragEventUseCase dragEventUseCase;
    /**
     * This is the use case that handles the click event
     * in the trade GUI.
     */
    private final @NonNull TradeClickEventUseCase clickEventUseCase;

    /**
     * Requests a trade with another player.
     * @param sender the player who is sending the trade request
     * @param recipientName the name of the player who will receive the trade request
     */
    public void request(@NonNull Player sender, @NonNull String recipientName) {
        try {
            this.requestUseCase.submit(sender, recipientName);
        } catch (Exception ex) {
            // Handle the exception
            this.handleException(sender, ex);
        }
    }

    /**
     * Accepts a trade request from a player.
     * @param player the player who is accepting the trade request
     * @param recipientName the name of the player who sent the trade request
     */
    public void accept(@NonNull Player player, @NonNull String recipientName) {
        try {
            this.acceptUseCase.submit(player, recipientName);
        } catch (Exception ex) {
            // Handle the exception
            this.handleException(player, ex);
        }
    }

    /**
     * Marks the player as ready to complete the trade.
     * @param player the player who is ready to complete the trade
     */
    public void ready(@NonNull Player player) {
        try {
            this.readyUseCase.submit(player);
        } catch (Exception ex) {
            // Handle the exception
            this.handleException(player, ex);
        }
    }

    /**
     * Cancels the trade transaction for a player.
     * @param player the player who wants to cancel the trade
     * @param closingInventory the inventory that is being closed
     */
    public void cancel(@NonNull Player player, @NonNull Inventory closingInventory) {
        try {
            this.cancelUseCase.submit(player, closingInventory);
        } catch (Exception ex) {
            // Handle the exception
            this.handleException(player, ex);
        }
    }

    /**
     * Handles the drag event in the trade GUI.
     * @param dragEvent the drag event that occurred in the trade GUI
     */
    public void dragEvent(@NonNull InventoryDragEvent dragEvent) {
        Player player = Optional.of(dragEvent.getWhoClicked())
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .orElse(null);
        if (player == null) {
            throw new IllegalArgumentException("The player who clicked the inventory must be a Player instance.");
        }

        try {
            dragEvent.setCancelled(this.dragEventUseCase.submit(player, dragEvent));
        } catch (Exception ex) {
            // Cancel the drag event to prevent further processing
            dragEvent.setCancelled(true);
            // Handle the exception
            this.handleException(player, ex);
        }
    }

    /**
     * Handles the click event in the trade GUI.
     * @param clickEvent the click event that occurred in the trade GUI
     */
    public void clickEvent(@NonNull InventoryClickEvent clickEvent) {
        Player player = Optional.of(clickEvent.getWhoClicked())
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .orElse(null);
        if (player == null) {
            throw new IllegalArgumentException("The player who clicked the inventory must be a Player instance.");
        }

        try {
            clickEvent.setCancelled(this.clickEventUseCase.submit(player, clickEvent));
        } catch (Exception ex) {
            // Cancel the click event to prevent further processing
            clickEvent.setCancelled(true);
            // Handle the exception
            this.handleException(player, ex);
        }
    }

    private void handleException(@NonNull Player player, @NonNull Exception ex) {
        // Log the exception if needed
        ex.printStackTrace(System.err);

        // Close the inventory if the transaction failed
        // but the close must be called on the next tick
        Bukkit.getScheduler().runTask(
                JavaPlugin.getPlugin(Trade.class),
                () -> player.closeInventory()
        );

        // Notify the player that an error occurred
        player.sendMessage(Component.text("An error occurred while processing your request. Please try again later.", NamedTextColor.RED));
    }
}