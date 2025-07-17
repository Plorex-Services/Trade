package it.bitrule.trade.component;

import com.google.gson.JsonObject;
import lombok.NonNull;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * @param slot         The slot index where the item stack was changed.
 * @param oldItemStack The old item stack before the change.
 * @param newItemStack The new item stack after the change.
 */
public record ChangedItemStack(
        int slot,
        @Nullable ItemStack oldItemStack,
        @Nullable ItemStack newItemStack,
        @NonNull ChangeType changeType
) {

    public @NonNull JsonObject body() {
        int oldAmount = this.oldItemStack != null ? this.oldItemStack.getAmount() : 0;
        int newAmount = this.newItemStack != null ? this.newItemStack.getAmount() : 0;

        int differenceAmount = newAmount - oldAmount;

        JsonObject body = new JsonObject();
        body.addProperty("slot", this.slot);

        body.addProperty("old_item_type", this.oldItemStack != null ? this.oldItemStack.getType().name() : "Unknown");
        body.addProperty("old_item_amount", oldAmount);
        body.addProperty("new_item_type", this.newItemStack != null ? this.newItemStack.getType().name() : "Unknown");
        body.addProperty("new_item_amount", newAmount);
        body.addProperty("difference_amount", differenceAmount);

        body.addProperty("type", this.changeType.name());

        return body;
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