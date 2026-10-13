package org.yuemi.mmoaccessories.api;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Definition of a single accessory slot, loaded from one entry under
 * {@code slots:} in {@code config.yml}.
 *
 * @param type          the slot id (built-in or custom)
 * @param displayName   human-readable name used in chat messages and lore
 * @param maxSlots      the number of inventory slots this type spans
 * @param acceptedTypes list of MMO Items types that may be placed in the
 *                      slot (any-of matching against
 *                      {@code MmoItemsApi.getItemTypes(item)})
 * @param gui           GUI config (slot positions, placeholder icon and
 *                      label); may be {@code null} for definitions that
 *                      don't render a GUI panel
 */
public record SlotDefinition(
    AccessorySlotId type,
    String displayName,
    int maxSlots,
    List<String> acceptedTypes,
    AccessorySlotGui gui
) {
    /**
     * Create a slot definition without GUI config.
     *
     * <p>Useful for code paths that operate on slot metadata only (e.g.
     * command listings) without needing to render a GUI.
     */
    public SlotDefinition(AccessorySlotId type, String displayName, int maxSlots, List<String> acceptedTypes) {
        this(type, displayName, maxSlots, acceptedTypes, null);
    }

    /**
     * @return the primary (first) accepted type, or the slot id if the
     *         {@code accepted-types} list is empty
     */
    public String primaryType() {
        return acceptedTypes.isEmpty() ? type.id() : acceptedTypes.get(0);
    }

    /**
     * Build a slot definition with a default GUI config generated from
     * {@code slots}, {@code icon} and {@code labelName}.
     *
     * @param type          the slot id
     * @param displayName   display name
     * @param maxSlots      maximum number of slots
     * @param acceptedTypes any-of matching accepted types
     * @param slots         inventory slot positions
     * @param icon          placeholder material key
     * @param labelName     MiniMessage display name for the placeholder
     * @return a new {@link SlotDefinition} with a default GUI
     */
    public static SlotDefinition of(
        AccessorySlotId type,
        String displayName,
        int maxSlots,
        @NotNull List<String> acceptedTypes,
        @NotNull List<Integer> slots,
        @NotNull String icon,
        @NotNull String labelName
    ) {
        return new SlotDefinition(
            type,
            displayName,
            maxSlots,
            acceptedTypes,
            AccessorySlotGui.defaultConfig(type, slots, icon, labelName)
        );
    }
}
