package it.bitrule.trade.component;

import lombok.NonNull;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bson.Document;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * @param slot         The slot index where the item stack was changed.
 * @param oldItemStack The old item stack before the change.
 * @param newItemStack The new item stack after the change.
 */
public record ChangedItemStack(
        int slot,
        @NonNull String playerName,
        @Nullable ItemStack oldItemStack,
        @Nullable ItemStack newItemStack,
        @NonNull ChangeType changeType
) {

    public @NonNull Document asDocument(int id) {
        StringBuilder builder = new StringBuilder(this.playerName).append(" ");
        if (this.changeType == ChangeType.ADD) {
            builder.append("added ");
        } else if (this.changeType == ChangeType.REMOVE) {
            builder.append("removed ");
        } else if (this.changeType == ChangeType.CHANGE) {
            builder.append("changed ");
        }

        int oldAmount = this.oldItemStack != null ? this.oldItemStack.getAmount() : 0;
        int newAmount = this.newItemStack != null ? this.newItemStack.getAmount() : 0;

        int differenceAmount = newAmount - oldAmount;

        builder.append("an item stack in slot ")
                .append(this.slot)
                .append(": [Old type=")
                .append(this.oldItemStack != null ? this.oldItemStack.getType().name() : "Unknown")
                .append(", Old Display Name=")
                .append(Optional.ofNullable(this.oldItemStack)
                        .map(ItemStack::getItemMeta)
                        .map(ItemMeta::displayName)
                        .map(displayName -> LegacyComponentSerializer.legacySection().serialize(displayName))
                        .orElse("None")
                )
                .append(", Old amount=")
                .append(oldAmount)
                .append(", New type=")
                .append(this.newItemStack != null ? this.newItemStack.getType().name() : "Unknown")
                .append(", New Display Name=")
                .append(Optional.ofNullable(this.newItemStack)
                        .map(ItemStack::getItemMeta)
                        .map(ItemMeta::displayName)
                        .map(displayName -> LegacyComponentSerializer.legacySection().serialize(displayName))
                        .orElse("None")
                )
                .append(", New amount=")
                .append(newAmount)
                .append(", Difference amount=")
                .append(differenceAmount)
                .append("]");

        return new Document("id", id).append("message", builder.toString());
    }

    public enum ChangeType {
        /**
         * The item stack was added to the slot.
         */
        ADD,
        /**
         * The item stack was removed from the slot.
         */
        REMOVE,
        /**
         * The item stack was changed in the slot.
         */
        CHANGE
    }

}