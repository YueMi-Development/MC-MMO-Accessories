package org.yuemi.mmoaccessories.plugin.config;

import org.yuemi.mmoaccessories.api.AccessorySlot;
import org.yuemi.mmoaccessories.api.SlotDefinition;

import java.util.EnumMap;
import java.util.Map;

public class SlotConfig {

    private final Map<AccessorySlot, SlotDefinition> slotDefinitions = new EnumMap<>(AccessorySlot.class);

    public SlotConfig() {
        setDefaultSlots();
    }

    public void loadFromConfig(org.bukkit.configuration.file.FileConfiguration config) {
        if (!config.contains("slot-types")) {
            setDefaultSlots();
            return;
        }

        for (AccessorySlot slot : AccessorySlot.values()) {
            String path = "slot-types." + slot.name();
            if (config.contains(path)) {
                String displayName = config.getString(path + ".display-name", slot.name());
                int maxSlots = config.getInt(path + ".max-slots", slot.getMaxSlots());
                String requiredType = config.getString(path + ".required-type", slot.getRequiredType());
                slotDefinitions.put(slot, new SlotDefinition(slot, displayName, maxSlots, requiredType));
            } else {
                slotDefinitions.put(slot, new SlotDefinition(slot, slot.name(), slot.getMaxSlots(), slot.getRequiredType()));
            }
        }

        for (String key : config.getConfigurationSection("slot-types").getKeys(false)) {
            try {
                AccessorySlot slot = AccessorySlot.valueOf(key);
            } catch (IllegalArgumentException e) {
                // Unknown slot type in config, skip
            }
        }
    }

    private void setDefaultSlots() {
        for (AccessorySlot slot : AccessorySlot.values()) {
            slotDefinitions.put(slot, new SlotDefinition(slot, slot.name(), slot.getMaxSlots(), slot.getRequiredType()));
        }
    }

    public SlotDefinition getSlotDefinition(AccessorySlot slot) {
        return slotDefinitions.getOrDefault(slot, new SlotDefinition(slot, slot.name(), 1, slot.name()));
    }

    public int getMaxSlots(AccessorySlot slot) {
        return getSlotDefinition(slot).maxSlots();
    }

    public String getRequiredType(AccessorySlot slot) {
        return getSlotDefinition(slot).requiredType();
    }

    public String getDisplayName(AccessorySlot slot) {
        return getSlotDefinition(slot).displayName();
    }

    public Map<AccessorySlot, SlotDefinition> getAllSlotDefinitions() {
        return Map.copyOf(slotDefinitions);
    }
}
