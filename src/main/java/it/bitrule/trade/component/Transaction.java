package it.bitrule.trade.component;

import lombok.Data;
import lombok.NonNull;

import java.util.UUID;

@Data
public final class Transaction {

    /**
     * The unique identifier for the transaction.
     * This helps to distinguish between different trade transactions.
     */
    private final @NonNull UUID id;
    /**
     * The mojang id of the player who sent the trade request.
     */
    private final @NonNull UUID sender;
    /**
     * The mojang id of the player who accepted the trade request.
     */
    private final @NonNull UUID receptor;

    /**
     * If the sender has marked their part of the trade as ready.
     */
    private boolean senderReady = false;
    /**
     * If the receptor has marked their part of the trade as ready.
     */
    private boolean receptorReady = false;

    /**
     * This flag indicates whether the sender is on queue to finish their
     * click on the trade menu.
     * So if the flag is true, means he already clicked an item and is waiting
     * to complete his task to avoid concurrency issues.
     */
    private boolean senderClicked = false;
    /**
     * This flag indicates whether the receptor is on queue to finish their
     * click on the trade menu.
     */
    private boolean receptorClicked = false;

    /**
     * This flag indicates whether the trade transaction is cancelled.
     * This can happen if either player closes the trade menu or someone disconnects during the trade.
     */
    private boolean cancelled = false;
    /**
     * This flag indicates whether the trade transaction is ended.
     * So this means that both players have confirmed their part of the trade and the countdown has expired.
     */
    private boolean ended = false;

    /**
     * Returns the reader state of the player in the transaction.
     * @param playerId the mojang id of the player to check the ready state for
     * @return true if the player has marked their part of the trade as done, false otherwise
     */
    public boolean getReadyState(@NonNull UUID playerId) {
        if (playerId.equals(this.sender)) return this.senderReady;

        return this.receptorReady;
    }

    /**
     * Returns the clicked state of the player in the transaction.
     * @param playerId the mojang id of the player to check the clicked state for
     * @return true if the player has clicked an item in the trade menu, false otherwise
     */
    public boolean getClickedValue(@NonNull UUID playerId) {
        if (playerId.equals(this.sender)) return this.senderClicked;

        return this.receptorClicked;
    }

    /**
     * Sets the clicked state of the player in the transaction.
     * This method is created to avoid unnecessary repetition of code
     * @param playerId the mojang id of the player to set the clicked state for
     * @param value the clicked state to set, true if the player has clicked an item in the trade menu, false otherwise
     */
    public void setClickedValue(@NonNull UUID playerId, boolean value) {
        if (playerId.equals(this.sender)) {
            this.senderClicked = value;
        } else {
            this.receptorClicked = value;
        }
    }
}