package it.bitrule.trade.manager;

import it.bitrule.trade.MessageAssets;
import it.bitrule.trade.usecase.TradeRequestUseCase;
import it.unimi.dsi.fastutil.Pair;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

@RequiredArgsConstructor
public final class TradeManager {

    private final @NonNull TradeRequestUseCase requestUseCase;

    /**
     * Processes a trade request from the sender to the receptor.
     * This method submits a trade request to the use case,
     * which will handle the logic of the request.
     * As last step, it will return the state of the request and the manager going to
     * handle the response from the use case to the sender, like sending a message
     * depending on the state of the request.
     * @param sender the player who is sending the trade request
     * @param receptorName the name of the player who is receiving the trade request
     */
    public void request(@NonNull Player sender, @NonNull String receptorName) {
        Player receptor = Bukkit.getPlayerExact(receptorName);
        if (receptor == null || !receptor.isConnected()) {
            sender.sendMessage(MessageAssets.PLAYER_NOT_ONLINE.build(receptorName));
            return;
        }

        TradeRequestUseCase.State state = this.requestUseCase.submit(sender.getUniqueId(), receptor.getUniqueId());

        // Notify the receptor about the new trade request sending a message.
        receptor.sendMessage(Component.join(
                JoinConfiguration.newlines(),
                MessageAssets.TRADE_REQUEST_RECEIVED.buildMany(
                        sender.getName(),
                        MessageAssets.internal("trade.request_received.accept_prompt")
                                .hoverEvent(HoverEvent.showText(
                                        MessageAssets.TRADE_REQUEST_RECEIVED_ACCEPT_HOVER.build(sender.getName())
                                ))
                                .clickEvent(ClickEvent.runCommand("/trade accept " + sender.getName()))
                        ,
                        MessageAssets.internal("trade.request_received.deny_prompt")
                                .hoverEvent(HoverEvent.showText(
                                        MessageAssets.TRADE_REQUEST_RECEIVED_DENY_HOVER.build(sender.getName())
                                ))
                                .clickEvent(ClickEvent.runCommand("/trade deny " + sender.getName()))
                )
        ));
    }

    public void accept(@NonNull Player sender, @NonNull String receptorName) {
        if (receptorName.isEmpty()) {
            sender.sendMessage(MessageAssets.TRADE_COMMAND_USAGE.build());
            return;
        }

        Player receptor = Bukkit.getPlayerExact(receptorName);
        if (receptor == null || !receptor.isConnected()) {
            sender.sendMessage(MessageAssets.PLAYER_NOT_ONLINE.build(receptorName));
            return;
        }
    }
}