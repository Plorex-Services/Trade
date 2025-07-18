package it.bitrule.trade.component;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class Log {

    private final int id;
    /**
     * Represents a log entry with a message.
     */
    private final @NonNull String message;
}