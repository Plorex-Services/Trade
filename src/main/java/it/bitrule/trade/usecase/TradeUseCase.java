package it.bitrule.trade.usecase;

import it.bitrule.trade.registry.RequestsRegistry;
import it.bitrule.trade.registry.TransactionRegistry;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.logging.Logger;

@RequiredArgsConstructor
abstract class TradeUseCase {

    /**
     * This registry is used to manage transactions between players.
     */
    protected final @NonNull TransactionRegistry transactionRegistry;
    /**
     * This registry is used to manage trade requests between players.
     */
    protected final @NonNull RequestsRegistry requestsRegistry;
    /**
     * This logger is used to log messages related to trade operations.
     */
    protected final @NonNull Logger logger;
}