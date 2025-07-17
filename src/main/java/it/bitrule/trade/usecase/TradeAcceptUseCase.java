package it.bitrule.trade.usecase;

import it.bitrule.trade.MessageAssets;
import it.bitrule.trade.Trade;
import it.bitrule.trade.component.Transaction;
import it.bitrule.trade.registry.RequestsRegistry;
import it.bitrule.trade.registry.TransactionRegistry;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Consumer;

public final class TradeAcceptUseCase extends TradeUseCase {

    public TradeAcceptUseCase(@NonNull TransactionRegistry transactionRegistry, @NonNull RequestsRegistry requestsRegistry) {
        super(transactionRegistry, requestsRegistry);
    }

    /**
     * Submits a trade acceptance request.
     * This method is used when a player accepts a trade request from another player.
     * The sender is the player who send the accept command, so on the trade logic is who is accepting the trade.
     * It checks if the sender is already trading with someone else,
     * if the receptor is online, and if the sender has a pending trade request with the receptor.
     * If all conditions are met, it proceeds to accept the trade request.
     *
     * @param sender the player who is accepting the trade request
     * @param receptorName the name of the player who sent the trade request
     */
    public void submit(@NonNull Player sender, @NonNull String receptorName) {
        if (receptorName.isEmpty()) {
            sender.sendMessage(MessageAssets.TRADE_COMMAND_USAGE.build());
            return;
        }

        // Check if the sender is already trading with someone else.
        if (this.transactionRegistry.findByPlayer(sender.getUniqueId()) != null) {
            sender.sendMessage(MessageAssets.TRADE_SENDER_ALREADY_TRADING.build());
            return;
        }

        // Check if the receptor is online.
        Player receptor = Bukkit.getPlayerExact(receptorName);
        if (receptor == null || !receptor.isConnected()) {
            sender.sendMessage(MessageAssets.PLAYER_NOT_ONLINE.build(receptorName));
            return;
        }

        // Check if the receptor is already trading with someone else.
        if (this.transactionRegistry.findByPlayer(receptor.getUniqueId()) != null) {
            sender.sendMessage(MessageAssets.TRADE_RECEPTOR_ALREADY_TRADING.build(receptorName));
            return;
        }

            // Check if the sender has a pending trade request with the receptor.
        if (!this.requestsRegistry.has(receptor.getUniqueId(), sender.getUniqueId())) {
            sender.sendMessage(MessageAssets.NO_REQUEST_FOUND.build(receptorName));
            return;
        }

        // Before creating a transaction, clean up the request registry for the sender and receptor.
        this.requestsRegistry.clearReceptors(sender.getUniqueId());
        this.requestsRegistry.clearReceptors(receptor.getUniqueId());

        this.requestsRegistry.clearSent(receptor.getUniqueId());
        this.requestsRegistry.clearSent(sender.getUniqueId());

        // Note: The logic about transaction when accepting a trade
        // the sender id at the transaction is who sent the request,
        // and the receptor id is who accepted the request.
        final Transaction transaction = new Transaction(UUID.randomUUID(), receptor.getUniqueId(), sender.getUniqueId());
        this.transactionRegistry.register(transaction);

        // Notify the receptor about the player has accepted his trade request.
        // The sender is the one who accepted the trade, so we send a message to the receptor
        receptor.sendMessage(MessageAssets.TRADE_REQUEST_WAS_ACCEPTED.build(sender.getName()));
        sender.sendMessage(MessageAssets.TRADE_REQUEST_ACCEPTED.build(receptor.getName()));

        Consumer<Player> playSound = player -> player.playSound(
                player.getLocation().clone(),
                Sound.ENTITY_PLAYER_LEVELUP,
                0.6f,
                1.0f
        );

        // Play sound for both sender and receptor to indicate the trade acceptance.
        playSound.accept(receptor);
        playSound.accept(sender);

        Trade.showGui(sender, transaction, receptor.getName());
        Trade.showGui(receptor, transaction, sender.getName());
    }
}