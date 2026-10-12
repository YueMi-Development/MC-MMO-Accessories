package org.yuemi.mmoaccessories.api;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * GUI configuration for an accessory slot type.
 * Defines where the slot is rendered in the inventory and its visual appearance.
 */
public record AccessorySlotGui(
    @NotNull List<Integer> slots,
    @NotNull String icon,
    @NotNull String labelName,
    @NotNull List<String> labelLore
) {
    /**
     * Create a default GUI config for a slot type.
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
