package org.yuemi.mmoaccessories.plugin.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.yuemi.mmoaccessories.api.AccessorySlot;
import org.yuemi.mmoaccessories.api.AccessorySlotGui;
import org.yuemi.mmoaccessories.api.AccessorySlotId;
import org.yuemi.mmoaccessories.api.SlotDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SlotConfig {

    private final Map<AccessorySlotId, SlotDefinition> slotDefinitions = new LinkedHashMap<>();

    public SlotConfig() {
        setDefaults();
    }

    public void loadFromConfig(FileConfiguration config) {
        slotDefinitions.clear();
        if (config.contains("slots")) {
            loadFromSlotsConfig(config);
        } else {
            setDefaults();
        }
    }

    private void loadFromSlotsConfig(FileConfiguration config) {
        // First load all built-in defaults so missing entries get defaults
        for (AccessorySlot slot : AccessorySlot.values()) {
            slotDefinitions.put(AccessorySlotId.of(slot), defaultDefinition(slot));
        }
        // Override with config values, and add custom slots
        for (String key : config.getConfigurationSection("slots").getKeys(false)) {
            String path = "slots." + key;
            AccessorySlotId slotId = AccessorySlotId.of(key);
            AccessorySlot builtin = slotId.builtin();

            int maxSlots = config.getInt(path + ".max-slots", builtin != null ? builtin.getMaxSlots() : 1);
            String displayName = config.getString(path + ".display-name", key);

            List<String> acceptedTypes = loadAcceptedTypes(config, path, slotId);
            AccessorySlotGui gui = loadGuiConfig(config, path + ".gui", slotId, maxSlots);
            slotDefinitions.put(slotId, new SlotDefinition(slotId, displayName, maxSlots, acceptedTypes, gui));
        }
    }

    private List<String> loadAcceptedTypes(FileConfiguration config, String path, AccessorySlotId slotId) {
        List<String> types = config.getStringList(path + ".accepted-types");
        if (types == null || types.isEmpty()) {
            // Fall back to slot id
            return List.of(slotId.id());
        }
        return types;
    }

    private AccessorySlotGui loadGuiConfig(FileConfiguration config, String path, AccessorySlotId slotId, int maxSlots) {
        if (!config.contains(path)) {
            return defaultGuiFor(slotId, maxSlots);
        }

        List<Integer> slots = new ArrayList<>();
        if (config.isConfigurationSection(path + ".slots")) {
            for (String key : config.getConfigurationSection(path + ".slots").getKeys(false)) {
                int pos = config.getInt(path + ".slots." + key, -1);
                if (pos >= 0) slots.add(pos);
            }
        }
        if (slots.isEmpty()) {
            for (int i = 0; i < maxSlots; i++) {
                slots.add(defaultGuiSlot(slotId, i));
            }
        }
        String icon = config.getString(path + ".icon", defaultIcon(slotId));
        String labelName = config.getString(path + ".label-name", defaultLabelName(slotId));
        List<String> labelLore = config.getStringList(path + ".label-lore");
        if (labelLore.isEmpty()) {
            labelLore = List.of(
                "<gray>Type: <green>" + slotId.id() + "</green></gray>"
            );
        }
        return new AccessorySlotGui(slots, icon, labelName, labelLore);
    }

    private SlotDefinition defaultDefinition(AccessorySlot slot) {
        List<String> accepted = List.of(slot.getRequiredType());
        return new SlotDefinition(
            AccessorySlotId.of(slot),
            slot.name(),
            slot.getMaxSlots(),
            accepted,
            defaultGuiFor(AccessorySlotId.of(slot), slot.getMaxSlots())
        );
    }

    private AccessorySlotGui defaultGuiFor(AccessorySlotId slotId, int maxSlots) {
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < maxSlots; i++) {
            positions.add(defaultGuiSlot(slotId, i));
        }
        return new AccessorySlotGui(
            positions,
            defaultIcon(slotId),
            defaultLabelName(slotId),
            List.of(
                "<gray>Type: <green>" + slotId.id() + "</green></gray>"
            )
        );
    }

    private int defaultGuiSlot(AccessorySlotId slotId, int index) {
        int hash = Math.abs(slotId.id().hashCode());
        int baseRow = (hash % 4) + 1;
        int baseCol = 1 + (index * 2) + (hash % 3);
        return baseRow * 9 + baseCol;
    }

    private String defaultIcon(AccessorySlotId slotId) {
        if (slotId.isBuiltin()) {
            return switch (slotId.builtin()) {
                case BELT -> "minecraft:light_gray_stained_glass_pane";
                case GLOVES -> "minecraft:white_stained_glass_pane";
                case RING -> "minecraft:yellow_stained_glass_pane";
                case NECKLACE -> "minecraft:orange_stained_glass_pane";
            };
        }
        return "minecraft:gray_stained_glass_pane";
    }

    private String defaultLabelName(AccessorySlotId slotId) {
        if (slotId.isBuiltin()) {
            return switch (slotId.builtin()) {
                case BELT -> "<gold><bold>Belt Slot</bold></gold>";
                case GLOVES -> "<aqua><bold>Gloves Slots</bold></aqua>";
                case RING -> "<yellow><bold>Ring Slots</bold></yellow>";
                case NECKLACE -> "<light_purple><bold>Necklace Slot</bold></light_purple>";
            };
        }
        return "<white><bold>" + slotId.id() + " Slot</bold></white>";
    }

    private void setDefaults() {
        slotDefinitions.clear();
        for (AccessorySlot slot : AccessorySlot.values()) {
            slotDefinitions.put(AccessorySlotId.of(slot), defaultDefinition(slot));
        }
    }

    public SlotDefinition getSlotDefinition(AccessorySlot slot) {
        return getSlotDefinition(AccessorySlotId.of(slot));
    }

    public SlotDefinition getSlotDefinition(AccessorySlotId slotId) {
        return slotDefinitions.getOrDefault(slotId, defaultDefinition(
            slotId.isBuiltin() ? slotId.builtin() : AccessorySlot.values()[0]));
    }

    public int getMaxSlots(AccessorySlot slot) {
        return getMaxSlots(AccessorySlotId.of(slot));
    }

    public int getMaxSlots(AccessorySlotId slotId) {
        return getSlotDefinition(slotId).maxSlots();
    }

    public String getRequiredType(AccessorySlot slot) {
        return getRequiredType(AccessorySlotId.of(slot));
    }

    public String getRequiredType(AccessorySlotId slotId) {
        return getSlotDefinition(slotId).primaryType();
    }

    public String getDisplayName(AccessorySlot slot) {
        return getDisplayName(AccessorySlotId.of(slot));
    }

    public String getDisplayName(AccessorySlotId slotId) {
        return getSlotDefinition(slotId).displayName();
    }

    public Map<AccessorySlotId, SlotDefinition> getAllSlotDefinitions() {
        return Map.copyOf(slotDefinitions);
    }

    public List<String> getAcceptedTypes(AccessorySlot slot) {
        return getAcceptedTypes(AccessorySlotId.of(slot));
    }

    public List<String> getAcceptedTypes(AccessorySlotId slotId) {
        return getSlotDefinition(slotId).acceptedTypes();
    }
}

