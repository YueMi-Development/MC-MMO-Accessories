package org.yuemi.mmoaccessories.api;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents the definition of an accessory slot with its configuration.
 */
public record SlotDefinition(
    AccessorySlotId type,
    String displayName,
    int maxSlots,
    String requiredType,
    AccessorySlotGui gui
) {
    /**
     * Create a slot definition without GUI config (backward compat).
     */
    public SlotDefinition(AccessorySlotId type, String displayName, int maxSlots, String requiredType) {
        this(type, displayName, maxSlots, requiredType, null);
    }

    public static SlotDefinition of(
        AccessorySlotId type,
        String displayName,
        int maxSlots,
        String requiredType,
        @NotNull List<Integer> slots,
        @NotNull String icon,
        @NotNull String labelName
    ) {
        return new SlotDefinition(
            type,
            displayName,
            maxSlots,
            requiredType,
            AccessorySlotGui.defaultConfig(type, slots, icon, labelName)
        );
    }
}
