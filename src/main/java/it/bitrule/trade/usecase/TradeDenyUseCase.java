package it.bitrule.trade.usecase;

import it.bitrule.trade.MessageAssets;
import it.bitrule.trade.registry.RequestsRegistry;
import it.bitrule.trade.registry.TransactionRegistry;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

public final class TradeDenyUseCase extends TradeUseCase {

    public TradeDenyUseCase(
            @NonNull TransactionRegistry transactionRegistry,
            @NonNull RequestsRegistry requestsRegistry,
            @NonNull Logger logger
    ) {
        super(transactionRegistry, requestsRegistry, logger);
    }

    public void submit(@NonNull Player sender, @NonNull String recipientName) {
        Player recipient = Bukkit.getPlayer(recipientName);
        if (recipient == null || !recipient.isOnline()) {
            sender.sendMessage(MessageAssets.PLAYER_NOT_ONLINE.build(recipientName));
            return;
        }

        if (!this.requestsRegistry.has(recipient.getUniqueId(), sender.getUniqueId())) {
            sender.sendMessage(MessageAssets.NO_REQUEST_FOUND.build(recipient.getName()));
            return;
        }

        this.requestsRegistry.unregister(recipient.getUniqueId(), sender.getUniqueId());

        recipient.sendMessage(MessageAssets.REQUEST_WAS_DENIED.build(sender.getName()));
        sender.sendMessage(MessageAssets.REQUEST_DENIED.build(recipient.getName()));
    }
}