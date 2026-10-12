package org.yuemi.mmoaccessories.plugin.config.migration;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.yuemi.config.api.MigrationStep;

import java.util.List;

/**
 * Migrates config from v2 (slot-types) to v3 (slots with gui sections).
 */
public class MigrationStep2To3 implements MigrationStep {

    private static final List<List<Integer>> DEFAULT_SLOT_POSITIONS = List.of(
        List.of(20),               // BELT
        List.of(21, 23),           // GLOVES
        List.of(30, 32),           // RING
        List.of(39)                // NECKLACE
    );

    private static final List<String> DEFAULT_ICONS = List.of(
        "minecraft:light_gray_stained_glass_pane",
        "minecraft:white_stained_glass_pane",
        "minecraft:yellow_stained_glass_pane",
        "minecraft:orange_stained_glass_pane"
    );

    private static final List<String> DEFAULT_LABELS = List.of(
        "<gold><bold>Belt Slot</bold></gold>",
        "<aqua><bold>Gloves Slots</bold></aqua>",
        "<yellow><bold>Ring Slots</bold></yellow>",
        "<light_purple><bold>Necklace Slot</bold></light_purple>"
    );

    private static final List<String> SLOT_NAMES = List.of("BELT", "GLOVES", "RING", "NECKLACE");

    @Override
    public int getTargetVersion() {
        return 3;
    }

    @Override
    public void migrate(@NotNull FileConfiguration config) {
        // Clear the old structure
        if (config.contains("slot-types")) {
            // Read each slot from old structure
            for (int i = 0; i < SLOT_NAMES.size(); i++) {
                String name = SLOT_NAMES.get(i);
                String oldPath = "slot-types." + name;
                if (config.contains(oldPath)) {
                    String displayName = config.getString(oldPath + ".display-name", name);
                    int maxSlots = config.getInt(oldPath + ".max-slots", 1);
                    String requiredType = config.getString(oldPath + ".required-type", name);

                    String newPath = "slots." + name;
                    config.set(newPath + ".display-name", displayName);
                    config.set(newPath + ".max-slots", maxSlots);
                    config.set(newPath + ".required-type", requiredType);

                    // Add default GUI section
                    config.set(newPath + ".gui.slots", DEFAULT_SLOT_POSITIONS.get(i));
                    config.set(newPath + ".gui.icon", DEFAULT_ICONS.get(i));
                    config.set(newPath + ".gui.label-name", DEFAULT_LABELS.get(i));
                    config.set(newPath + ".gui.label-lore", List.of(
                        "<gray>Type: <green>" + requiredType + "</green></gray>"
                    ));
                }
            }
            config.set("slot-types", null);
        }

        // Ensure GUI section exists with defaults
        if (!config.contains("gui")) {
            config.set("gui.title", "Accessories");
            config.set("gui.rows", 6);
            config.set("gui.background.enabled", true);
            config.set("gui.background.icon", "minecraft:black_stained_glass_pane");
            config.set("gui.action-buttons.close.slot", 53);
            config.set("gui.action-buttons.close.icon", "minecraft:arrow");
            config.set("gui.action-buttons.close.name", "<red><bold>Close</bold></red>");
            config.set("gui.action-buttons.close.lore", List.of("<gray>Close this menu</gray>"));
        }
    }
}
