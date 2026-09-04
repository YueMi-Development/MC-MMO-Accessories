package org.yuemi.mmoaccessories.api;

/**
 * Accessory slot types with their max slot counts and allowed item types.
 */
public enum AccessorySlot {
    BELT(1, "BELT"),
    GLOVES(2, "GLOVES"),
    RING(2, "RING"),
    NECKLACE(1, "NECKLACE");

    private final int maxSlots;
    private final String requiredType;

    AccessorySlot(int maxSlots, String requiredType) {
        this.maxSlots = maxSlots;
        this.requiredType = requiredType;
    }

    public int getMaxSlots() {
        return maxSlots;
    }

    public String getRequiredType() {
        return requiredType;
    }
}
