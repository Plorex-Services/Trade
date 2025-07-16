package it.bitrule.trade.usecase;

import it.bitrule.trade.registry.RequestsRegistry;
import it.bitrule.trade.registry.TransactionRegistry;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

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
}