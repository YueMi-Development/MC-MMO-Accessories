package org.yuemi.mmoaccessories.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * API for managing player accessories (equipment slots).
 */
public interface AccessoriesApi {

    /**
     * Equip an accessory to the specified slot.
     * Item must have the correct type for the slot.
     */
    boolean equipAccessory(@NotNull Player player, @NotNull AccessorySlot slot, int slotIndex, @NotNull ItemStack item);

    /**
     * Unequip an accessory from the specified slot.
     */
    boolean unequipAccessory(@NotNull Player player, @NotNull AccessorySlot slot, int slotIndex);

    /**
     * Get the equipped item in a specific slot.
     */
    @NotNull Optional<ItemStack> getEquipped(@NotNull Player player, @NotNull AccessorySlot slot, int slotIndex);

    /**
     * Get all equipped accessories for a player.
     */
    @NotNull List<ItemStack> getAllEquipped(@NotNull Player player);

    /**
     * Check if an item can be equipped to a specific slot.
     * Item must have the matching type (e.g., "BELT" for BELT slot).
     */
    boolean canEquipToSlot(@NotNull ItemStack item, @NotNull AccessorySlot slot);

    /**
     * Check if an item has any accessory type.
     */
    boolean isAccessoryType(@NotNull ItemStack item);

    /**
     * Check if a slot is full.
     */
    boolean isSlotFull(@NotNull Player player, @NotNull AccessorySlot slot);

    /**
     * Get the required item type for a slot.
     */
    @NotNull String getSlotRequiredType(@NotNull AccessorySlot slot);

    int getMaxSlots(@NotNull AccessorySlot slot);

    boolean isAvailable();

    void sendMessage(@NotNull Player player, @NotNull String message);

    boolean isFeatureEnabled(@NotNull Player player);

    /**
     * Open the accessories GUI for the player.
     */
    void openGui(@NotNull Player player);
}
