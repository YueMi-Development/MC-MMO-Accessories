package org.yuemi.mmoaccessories.api;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * GUI configuration for a single accessory slot type.
 *
 * <p>Loaded from the {@code gui} sub-block of each slot in
 * {@code config.yml}. The default values for built-in slots are generated
 * by {@link #defaultConfig(AccessorySlotId, List, String, String)} when the
 * config doesn't override them.
 *
 * @param slots      inventory slot positions (zero-indexed) inside the chest
 *                   GUI where this slot type's items are placed. The list
 *                   length should equal the slot's {@code max-slots}.
 * @param icon       material key (vanilla id or {@code namespace:id}) for the
 *                   empty-slot placeholder
 * @param labelName  MiniMessage display name for the empty placeholder
 * @param labelLore  MiniMessage lore lines for the empty placeholder
 */
public record AccessorySlotGui(
    @NotNull List<Integer> slots,
    @NotNull String icon,
    @NotNull String labelName,
    @NotNull List<String> labelLore
) {
    /**
     * Create a default GUI config for a slot type.
     *
     * <p>Generates a single lore line of the form
     * {@code "<gray>Type: <green><id></green></gray>"} using the slot's id.
     *
     * @param slot      the slot id (used for the default lore line)
     * @param positions inventory slot positions
     * @param icon      material key for the placeholder
     * @param labelName MiniMessage display name for the placeholder
     * @return a new {@link AccessorySlotGui} with default lore
     */
    public static AccessorySlotGui defaultConfig(
        @NotNull AccessorySlotId slot,
        @NotNull List<Integer> positions,
        @NotNull String icon,
        @NotNull String labelName
    ) {
        return new AccessorySlotGui(
            positions,
            icon,
            labelName,
            List.of(
                "<gray>Type: <green>" + slot.id() + "</green></gray>"
            )
        );
    }
}
