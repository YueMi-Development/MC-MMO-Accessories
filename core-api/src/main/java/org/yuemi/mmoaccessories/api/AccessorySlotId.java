package org.yuemi.mmoaccessories.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Identifier for an accessory slot type.
 *
 * <p>Can be either a built-in {@link AccessorySlot} enum value or a custom
 * slot id defined in {@code config.yml} under {@code slots:}. Custom slots
 * use the string form returned by {@link #id()}; their {@link #builtin()}
 * is {@code null}.
 */
public final class AccessorySlotId {

    private final String id;
    private final AccessorySlot builtin;

    private AccessorySlotId(String id, AccessorySlot builtin) {
        this.id = id;
        this.builtin = builtin;
    }

    /**
     * Wrap a built-in slot enum as an {@link AccessorySlotId}.
     *
     * @param slot the built-in slot
     * @return a slot id whose {@link #id()} equals {@code slot.name()}
     */
    public static AccessorySlotId of(@NotNull AccessorySlot slot) {
        return new AccessorySlotId(slot.name(), slot);
    }

    /**
     * Parse a slot id string. If {@code id} matches a built-in
     * {@link AccessorySlot} name (case-insensitive), the resulting
     * {@link AccessorySlotId} carries that enum value as its
     * {@link #builtin()}; otherwise {@link #builtin()} is {@code null} and the
     * id is treated as a custom slot.
     *
     * @param id the slot identifier (e.g. {@code "BELT"} or a custom id)
     * @return a slot id with the uppercased name normalised
     */
    public static AccessorySlotId of(@NotNull String id) {
        try {
            return new AccessorySlotId(id.toUpperCase(), AccessorySlot.valueOf(id.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return new AccessorySlotId(id.toUpperCase(), null);
        }
    }

    /**
     * @return the slot identifier, normalised to upper case
     */
    @NotNull
    public String id() {
        return id;
    }

    /**
     * @return the matching built-in slot, or {@code null} if this id
     *         represents a custom slot
     */
    @Nullable
    public AccessorySlot builtin() {
        return builtin;
    }

    /**
     * @return {@code true} if this id corresponds to a built-in slot
     */
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
