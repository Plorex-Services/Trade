package it.bitrule.trade.usecase;

import it.bitrule.trade.MessageAssets;
import it.bitrule.trade.registry.RequestsRegistry;
import it.bitrule.trade.registry.TransactionRegistry;
import it.unimi.dsi.fastutil.Pair;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.InputMismatchException;
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
     * @param senderEntry a pair containing the UUID of the sender and their name
     * @param receptorName the name of the player receiving the trade request
     * @return the state of the trade request submission
     */
    public @NonNull State submit(@NonNull Pair<UUID, String> senderEntry, @NonNull String receptorName) {
        String senderName = senderEntry.second();
        if (senderName == null || senderName.isEmpty()) {
            throw new InputMismatchException("Sender name cannot be empty");
        }

        // Check if the sender is already trading with someone else.
        if (this.transactionRegistry.findByPlayer(senderEntry.first()) != null)
            return State.SENDER_ALREADY_TRADING;

        Player receptor = Bukkit.getPlayerExact(receptorName);
        if (receptor == null || !receptor.isConnected())
            return State.NO_RECEPTOR_ONLINE;

        // Check if the receptor is trying to trade with themselves.
        if (receptor.getUniqueId().equals(senderEntry.first()))
            return State.TRYING_TO_TRADE_SELF;

        // Check if the receptor is already trading with someone else.
        if (this.transactionRegistry.findByPlayer(receptor.getUniqueId()) != null)
            return State.RECEPTOR_ALREADY_TRADING;

        // If the receptor already has a pending request from the sender,
        // we return ALREADY_SENT_REQUEST state.
        if (this.requestsRegistry.has(senderEntry.first(), receptor.getUniqueId()))
            return State.SENDER_ALREADY_SENT_REQUEST;

        // If the sender already has a pending request from the receptor,
        // we return ALREADY_SENT_REQUEST state.
        if (this.requestsRegistry.has(receptor.getUniqueId(), senderEntry.first()))
            return State.RECEPTOR_ALREADY_SENT_REQUEST;

        // Register the request in the registry.
        this.requestsRegistry.register(senderEntry.first(), receptor.getUniqueId());

        // Notify the receptor about the new trade request sending a message.
        receptor.sendMessage(Component.join(
                JoinConfiguration.newlines(),
                MessageAssets.TRADE_REQUEST_RECEIVED.buildMany(
                        senderName,
                        MessageAssets.internal("trade.request_received.accept_prompt")
                                .hoverEvent(HoverEvent.showText(
                                        MessageAssets.TRADE_REQUEST_RECEIVED_ACCEPT_HOVER.build(senderName)
                                ))
                                .clickEvent(ClickEvent.runCommand("/trade accept " + senderName))
                        ,
                        MessageAssets.internal("trade.request_received.deny_prompt")
                                .hoverEvent(HoverEvent.showText(
                                        MessageAssets.TRADE_REQUEST_RECEIVED_DENY_HOVER.build(senderName)
                                ))
                                .clickEvent(ClickEvent.runCommand("/trade deny " + senderName))
                )
        ));

        return State.OK;
    }
}