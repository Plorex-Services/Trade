package it.bitrule.trade.manager;

import it.bitrule.trade.Trade;
import it.bitrule.trade.listener.InventoryCloseListener;
import it.bitrule.trade.listener.PlayerQuitListener;
import it.bitrule.trade.registry.RequestsRegistry;
import it.bitrule.trade.registry.TransactionRegistry;
import it.bitrule.trade.usecase.*;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class TradeManagerFactory {

    public static @NonNull TradeManager create() {
        TransactionRegistry transactionRegistry = new TransactionRegistry();
        RequestsRegistry requestsRegistry = new RequestsRegistry();

        JavaPlugin plugin = JavaPlugin.getPlugin(Trade.class);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(requestsRegistry), plugin);
        Bukkit.getPluginManager().registerEvents(new InventoryCloseListener(), plugin);

        return new TradeManager(
                new TradeRequestUseCase(transactionRegistry, requestsRegistry, plugin.getLogger()),
                new TradeAcceptUseCase(transactionRegistry, requestsRegistry, plugin.getLogger()),
                new TradeReadyUseCase(transactionRegistry, requestsRegistry, plugin.getLogger()),
                new TradeCancelUseCase(transactionRegistry, requestsRegistry, plugin.getLogger()),
                new TradeDragEventUseCase(transactionRegistry, requestsRegistry, plugin.getLogger()),
                new TradeClickEventUseCase(transactionRegistry, requestsRegistry, plugin.getLogger())
        );
    }
}