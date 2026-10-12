package org.yuemi.mmoaccessories.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Identifier for an accessory slot type. Can be either a built-in {@link AccessorySlot}
 * enum value or a custom slot ID defined in config.
 */
public final class AccessorySlotId {

    private final String id;
    private final AccessorySlot builtin;

    private AccessorySlotId(String id, AccessorySlot builtin) {
        this.id = id;
        this.builtin = builtin;
    }

    public static AccessorySlotId of(@NotNull AccessorySlot slot) {
        return new AccessorySlotId(slot.name(), slot);
    }

    public static AccessorySlotId of(@NotNull String id) {
        try {
            return new AccessorySlotId(id.toUpperCase(), AccessorySlot.valueOf(id.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return new AccessorySlotId(id.toUpperCase(), null);
        }
    }

    @NotNull
    public String id() {
        return id;
    }

    @Nullable
    public AccessorySlot builtin() {
        return builtin;
    }

    public boolean isBuiltin() {
        return builtin != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccessorySlotId other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}
