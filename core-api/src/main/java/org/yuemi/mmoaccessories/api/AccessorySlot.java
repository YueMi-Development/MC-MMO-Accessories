package org.yuemi.mmoaccessories.api;

/**
 * Built-in accessory slot types.
 *
 * <p>Each constant defines the maximum number of slots for that type
 * ({@code max-slots} in {@code config.yml}) and the canonical
 * {@code required-type} string used to render the slot's display name
 * (e.g. "Belt" for {@code BELT}). The {@code accepted-types} list in
 * {@code config.yml} may add additional matching types — for example the
 * default {@code BELT} slot accepts both {@code BELT} and {@code WAIST}.
 *
 * <p>Custom slot ids (any non-built-in string) are supported through
 * {@link AccessorySlotId}.
 */
public enum AccessorySlot {

    /** Single belt slot. Default accepted types: {@code BELT}, {@code WAIST}. */
    BELT(1, "BELT"),

    /** Two hand-equipment slots. Default accepted types: {@code GLOVES}, {@code HAND}. */
    GLOVES(2, "GLOVES"),

    /** Two ring slots. Default accepted types: {@code RING}. */
    RING(2, "RING"),

    /** Single amulet slot. Default accepted types: {@code NECKLACE}, {@code AMULET}. */
    NECKLACE(1, "NECKLACE");

    private final int maxSlots;
    private final String requiredType;

    AccessorySlot(int maxSlots, String requiredType) {
        this.maxSlots = maxSlots;
        this.requiredType = requiredType;
    }

    /**
     * @return the maximum number of slots for this type
     */
    public int getMaxSlots() {
        return maxSlots;
    }

    /**
     * @return the canonical type tag for this slot, used as the
     *         {@code display-name} key in {@code config.yml} and in
     *         compatibility messaging
     */
    public String getRequiredType() {
        return requiredType;
    }
}
