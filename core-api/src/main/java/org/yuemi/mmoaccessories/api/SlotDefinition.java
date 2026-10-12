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
    List<String> acceptedTypes,
    AccessorySlotGui gui
) {
    /**
     * Create a slot definition without GUI config (backward compat).
     */
    public SlotDefinition(AccessorySlotId type, String displayName, int maxSlots, List<String> acceptedTypes) {
        this(type, displayName, maxSlots, acceptedTypes, null);
    }

    /**
     * Get the primary/display type (first item in acceptedTypes).
     * Used for display in lore, etc.
     */
    public String primaryType() {
        return acceptedTypes.isEmpty() ? type.id() : acceptedTypes.get(0);
    }

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
