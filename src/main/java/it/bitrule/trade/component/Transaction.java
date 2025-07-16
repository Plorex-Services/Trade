package it.bitrule.trade.component;

import lombok.Data;
import lombok.NonNull;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
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
     * The items that the sender is offering in the trade.
     */
    private final @NonNull Map<Integer, ItemStack> senderItems = new HashMap<>();
    /**
     * The items that the receptor is offering in the trade.
     */
    private final @NonNull Map<Integer, ItemStack> receptorItems = new HashMap<>();

    /**
     * The amount of money that the sender is offering in the trade.
     */
    private int senderMoney = 0;
    /**
     * The amount of money that the receptor is offering in the trade.
     */
    private int receptorMoney = 0;

    /**
     * If the sender has marked their part of the trade as done.
     */
    private boolean senderDone = false;
    /**
     * If the receptor has marked their part of the trade as done.
     */
    private boolean receptorDone = false;
}