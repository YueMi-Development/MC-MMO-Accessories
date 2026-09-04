package org.yuemi.mmoaccessories.api;

/**
 * Represents the definition of an accessory slot with its configuration.
 */
public record SlotDefinition(
    AccessorySlot type,
    String displayName,
    int maxSlots,
    String requiredType
) {}
