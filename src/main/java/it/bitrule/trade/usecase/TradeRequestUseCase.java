package it.bitrule.trade.usecase;

import it.bitrule.trade.registry.RequestsRegistry;
import it.bitrule.trade.registry.TransactionRegistry;
import lombok.NonNull;

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

    /**
     * Submits a trade request from the sender to the receptor.
     * This method checks various conditions to ensure that the trade request can be processed,
     * such as whether the sender and receptor are online, whether they are already trading with someone else,
     * and whether they have already sent or received trade requests from each other.
     * @param senderId the UUID of the player sending the trade request
     * @param receptorId the UUID of the player receiving the trade request
     * @return the state of the trade request submission
     */
    public @NonNull State submit(@NonNull UUID senderId, @NonNull UUID receptorId) {
        // Check if the sender is already trading with someone else.
        if (this.transactionRegistry.findByPlayer(senderId) != null)
            return State.SENDER_ALREADY_TRADING;

        // Check if the receptor is trying to trade with themselves.
        if (receptorId.equals(senderId))
            return State.TRYING_TO_TRADE_SELF;

        // Check if the receptor is already trading with someone else.
        if (this.transactionRegistry.findByPlayer(receptorId) != null)
            return State.RECEPTOR_ALREADY_TRADING;

        // If the receptor already has a pending request from the sender,
        // we return ALREADY_SENT_REQUEST state.
        if (this.requestsRegistry.has(senderId, receptorId))
            return State.SENDER_ALREADY_SENT_REQUEST;

        // If the sender already has a pending request from the receptor,
        // we return ALREADY_SENT_REQUEST state.
        if (this.requestsRegistry.has(receptorId, senderId))
            return State.RECEPTOR_ALREADY_SENT_REQUEST;

        // Register the request in the registry.
        this.requestsRegistry.register(senderId, receptorId);

        return State.OK;
    }
}