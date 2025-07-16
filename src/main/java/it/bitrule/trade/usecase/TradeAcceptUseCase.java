package it.bitrule.trade.usecase;

import it.bitrule.trade.MessageAssets;
import it.bitrule.trade.component.Transaction;
import it.bitrule.trade.registry.RequestsRegistry;
import it.bitrule.trade.registry.TransactionRegistry;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

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
        if (!this.requestsRegistry.has(sender.getUniqueId(), receptor.getUniqueId())) {
            sender.sendMessage(MessageAssets.TRADE_NO_PENDING_REQUEST.build(receptorName));
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
        this.transactionRegistry.register(new Transaction(UUID.randomUUID(), receptor.getUniqueId(), sender.getUniqueId()));
    }
}