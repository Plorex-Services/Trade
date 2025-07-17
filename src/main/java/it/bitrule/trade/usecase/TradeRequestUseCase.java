package it.bitrule.trade.usecase;

import it.bitrule.trade.MessageAssets;
import it.bitrule.trade.registry.RequestsRegistry;
import it.bitrule.trade.registry.TransactionRegistry;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class TradeRequestUseCase extends TradeUseCase {

    public TradeRequestUseCase(@NonNull TransactionRegistry transactionRegistry, @NonNull RequestsRegistry requestsRegistry) {
        super(transactionRegistry, requestsRegistry);
    }

    /**
     * Submits a trade request from the sender to the receptor.
     * This method checks various conditions to ensure that the trade request can be processed,
     * such as whether the sender and receptor are online, whether they are already trading with someone else,
     * and whether they have already sent or received trade requests from each other.
     * @param sender the player who is sending the trade request
     * @param receptorName the name of the player who is receiving the trade request
     */
    public void submit(@NonNull Player sender, @NonNull String receptorName) {
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

        // Check if the sender is trying to trade with themselves.
        if (receptor.getUniqueId().equals(sender.getUniqueId())) {
            sender.sendMessage(MessageAssets.TRADE_CANNOT_TRADE_YOURSELF.build());
            return;
        }

        // Check if the receptor is already trading with someone else.
        if (this.transactionRegistry.findByPlayer(receptor.getUniqueId()) != null) {
            sender.sendMessage(MessageAssets.TRADE_RECEPTOR_ALREADY_TRADING.build());
            return;
        }

        // Check if the sender has already sent a request to the receptor.
        if (this.requestsRegistry.has(sender.getUniqueId(), receptor.getUniqueId())) {
            sender.sendMessage(MessageAssets.TRADE_SENDER_ALREADY_SENT_REQUEST.build(receptorName));
            return;
        }

        // Check if the receptor has already sent a request to the sender.
        if (this.requestsRegistry.has(receptor.getUniqueId(), sender.getUniqueId())) {
            sender.sendMessage(MessageAssets.TRADE_RECEPTOR_ALREADY_SENT_REQUEST.build(receptorName));
            return;
        }

        // Register the request in the registry.
        this.requestsRegistry.register(sender.getUniqueId(), receptor.getUniqueId());

        // Notify the receptor about the new trade request sending a message.
        receptor.sendMessage(Component.join(
                JoinConfiguration.newlines(),
                MessageAssets.TRADE_REQUEST_RECEIVED.buildMany(
                        sender.getName(),
                        MessageAssets.internal("request_received.accept_prompt")
                                .hoverEvent(HoverEvent.showText(
                                        MessageAssets.TRADE_REQUEST_RECEIVED_ACCEPT_HOVER.build(sender.getName())
                                ))
                                .clickEvent(ClickEvent.runCommand("/trade accept " + sender.getName()))
                        ,
                        MessageAssets.internal("request_received.deny_prompt")
                                .hoverEvent(HoverEvent.showText(
                                        MessageAssets.TRADE_REQUEST_RECEIVED_DENY_HOVER.build(sender.getName())
                                ))
                                .clickEvent(ClickEvent.runCommand("/trade deny " + sender.getName()))
                )
        ));

        sender.sendMessage(MessageAssets.TRADE_REQUEST_SENT.build(receptorName));
    }
}