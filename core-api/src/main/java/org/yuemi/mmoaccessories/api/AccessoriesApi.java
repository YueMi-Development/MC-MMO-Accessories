package org.yuemi.mmoaccessories.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Public API for managing player accessories (equipment slots).
 *
 * <p>An accessory slot is identified by an {@link AccessorySlot} (or a custom
 * slot id when the slot is defined in {@code config.yml}). Items are validated
 * against the slot's {@code accepted-types} via MMO Items
 * ({@code MmoItemsApi.getItemTypes}).
 *
 * <p>Equip / unequip events flow through MMO Mechanics'
 * {@code MmoMechanicsApi.fireTrigger(UUID, "onEquip"/"onUnequip")} so any skill
 * declaring a matching trigger in its JSON5 {@code triggers[]} list runs as
 * part of the same logical operation. In-slot triggers (e.g. {@code onAttack}
 * on a worn ring) are dispatched by the accessories plugin's
 * {@code EventApi}-subscribed listener through
 * {@code MmoMechanicsApi.castSkill}.
 *
 * <p>Storage is pluggable via the {@link org.yuemi.mmoaccessories.api.storage.AccessoryStorage}
 * SPI. The default backend is {@code pdc} (player
 * {@code PersistentDataContainer}); {@code sqlite}, {@code mysql} and
 * {@code mongodb} are optional backends. Select the backend in
 * {@code config.yml} under {@code storage.type}.
 */
public interface AccessoriesApi {

    /**
     * Equip {@code item} to {@code (slot, slotIndex)} for {@code player}.
     *
     * <p>Returns {@code false} if any precondition fails:
     * <ul>
     *   <li>{@code slotIndex} is out of range for the slot's {@code max-slots}.</li>
     *   <li>{@code !canEquipToSlot(item, slot)}.</li>
     *   <li>The item cannot be resolved to a stored id (no MMO Items id and no
     *       vanilla material fallback).</li>
     * </ul>
     *
     * <p>On success the access is persisted, then the {@code onEquip} trigger
     * is dispatched through MMO Mechanics.
     *
     * @param player    the target player
     * @param slot      which accessory slot
     * @param slotIndex zero-based index inside the slot (must be &lt; {@code max-slots})
     * @param item      the item to place in the slot
     * @return {@code true} if the item was successfully equipped
     */
    boolean equipAccessory(@NotNull Player player, @NotNull AccessorySlot slot, int slotIndex, @NotNull ItemStack item);

    /**
     * Remove the accessory at {@code (slot, slotIndex)} for {@code player}.
     *
     * <p>On success the {@code onUnequip} trigger is dispatched through MMO
     * Mechanics.
     *
     * @return {@code true} if a slot was actually cleared (i.e. the slot was
     *         non-empty); {@code false} if the slot index was out of range or
     *         the slot was already empty
     */
    boolean unequipAccessory(@NotNull Player player, @NotNull AccessorySlot slot, int slotIndex);

    /**
     * Read back the equipped item at {@code (slot, slotIndex)}.
     *
     * <p>The returned item is reconstructed from the storage id (so a
     * freshly-spawned item will be a new instance, not a reference to whatever
     * the player had when equipped).
     *
     * @return the item, or empty if the slot is empty / out of range / the
     *         stored id can't be resolved
     */
    @NotNull Optional<ItemStack> getEquipped(@NotNull Player player, @NotNull AccessorySlot slot, int slotIndex);

    /**
     * Read back every equipped accessory for {@code player}, across all slots.
     *
     * <p>The list contains only non-empty slots and preserves slot ordering
     * (BELT, GLOVES x max, RING x max, NECKLACE). Empty slots are skipped.
     */
    @NotNull List<ItemStack> getAllEquipped(@NotNull Player player);

    /**
     * Check whether {@code item} can be placed in {@code slot}.
     *
     * <p>Returns {@code true} when the item's MMO Items types (read via
     * {@code MmoItemsApi.getItemTypes(item)}) include any of the slot's
     * {@code accepted-types} entries. Falls back to a material-name match
     * when MMO Items isn't installed or returns an empty type list.
     */
    boolean canEquipToSlot(@NotNull ItemStack item, @NotNull AccessorySlot slot);

    /**
     * @return {@code true} if {@code item} carries the {@code ACCESSORY} type
     *         in its MMO Items type list
     */
    boolean isAccessoryType(@NotNull ItemStack item);

    /**
     * @return {@code true} if every {@code max-slots} index in {@code slot}
     *         for {@code player} is filled
     */
    boolean isSlotFull(@NotNull Player player, @NotNull AccessorySlot slot);

    /**
     * @return the first entry in the slot's {@code accepted-types} list (the
     *         "primary" type used for display)
     */
    @NotNull String getSlotRequiredType(@NotNull AccessorySlot slot);

    /**
     * @return the configured {@code max-slots} for {@code slot}
     */
    int getMaxSlots(@NotNull AccessorySlot slot);

    /**
     * @return {@code true} when the API implementation is wired up and ready
     *         to handle requests (e.g. after a successful {@code onEnable})
     */
    boolean isAvailable();

    /**
     * Send a MiniMessage-formatted chat message to {@code player} using the
     * plugin's own messaging pipeline.
     */
    void sendMessage(@NotNull Player player, @NotNull String message);

    /**
     * @return {@code true} if {@code player} has the {@code accessories.feature}
     *         permission and should be considered an active user of the
     *         accessory system
     */
    boolean isFeatureEnabled(@NotNull Player player);

    /**
     * Open the accessories GUI for {@code player} (player sender only).
     * Sends an error message and returns silently if the GUI subsystem
     * is not available.
     */
    void openGui(@NotNull Player player);
}
