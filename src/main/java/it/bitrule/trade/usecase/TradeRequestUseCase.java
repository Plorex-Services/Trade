package it.bitrule.trade.usecase;

import it.bitrule.trade.registry.RequestsRegistry;
import it.bitrule.trade.registry.TransactionRegistry;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class TradeRequestUseCase extends TradeUseCase {

    public enum State {
        /**
         * This indicates that the sender is already trading with someone else.
         */
        SENDER_ALREADY_TRADING,
        /**
         * This indicates that the receptor is already trading with someone else.
         */
        RECEPTOR_ALREADY_TRADING,

        /**
         * This indicates that the receptor is not online
         */
        NO_RECEPTOR_ONLINE,
        /**
         * This indicates that the sender has already sent a request to the receptor.
         */
        SENDER_ALREADY_SENT_REQUEST,
        /**
         * This indicates that the receptor has already sent a request to the sender,
         * so the sender needs to accept the request, not send a new one.
         */
        RECEPTOR_ALREADY_SENT_REQUEST,
        /**
         * This indicates that the sender is trying to trade with themselves,
         * which is not allowed.
         */
        TRYING_TO_TRADE_SELF,
        /**
         * This indicates that the request was successfully registered
         */
        OK
    }

    public TradeRequestUseCase(@NonNull TransactionRegistry transactionRegistry, @NonNull RequestsRegistry requestsRegistry) {
        super(transactionRegistry, requestsRegistry);
    }

    public @NonNull State submit(@NonNull UUID senderId, @NonNull String receptorName) {
        // Check if the sender is already trading with someone else.
        if (this.transactionRegistry.findByPlayer(senderId) != null)
            return State.SENDER_ALREADY_TRADING;

        Player receptor = Bukkit.getPlayerExact(receptorName);
        if (receptor == null || !receptor.isConnected())
            return State.NO_RECEPTOR_ONLINE;

        // Check if the receptor is trying to trade with themselves.
        if (receptor.getUniqueId().equals(senderId))
            return State.TRYING_TO_TRADE_SELF;

        // Check if the receptor is already trading with someone else.
        if (this.transactionRegistry.findByPlayer(receptor.getUniqueId()) != null)
            return State.RECEPTOR_ALREADY_TRADING;

        // If the receptor already has a pending request from the sender,
        // we return ALREADY_SENT_REQUEST state.
        if (this.requestsRegistry.has(senderId, receptor.getUniqueId()))
            return State.SENDER_ALREADY_SENT_REQUEST;

        // If the sender already has a pending request from the receptor,
        // we return ALREADY_SENT_REQUEST state.
        if (this.requestsRegistry.has(receptor.getUniqueId(), senderId))
            return State.RECEPTOR_ALREADY_SENT_REQUEST;

        // Register the request in the registry.

        return State.OK;
    }
}