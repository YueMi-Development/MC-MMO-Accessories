package org.yuemi.mmoaccessories.plugin.config.migration;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.yuemi.config.api.MigrationStep;

/**
 * Migrates config from v1 (no slot config) to v2 (introduces a
 * {@code slot-types} block with default entries for the four built-in
 * slot types).
 *
 * <p>Adds {@code slot-types.BELT/GLOVES/RING/NECKLACE} entries with their
 * default {@code display-name}, {@code max-slots} and {@code required-type}
 * values if the {@code slot-types} block is missing.
 */
public class MigrationStep1To2 implements MigrationStep {

    @Override
    public int getTargetVersion() {
        return 2;
    }

    @Override
    public void migrate(@NotNull FileConfiguration config) {
        if (!config.contains("slot-types")) {
            config.set("slot-types.BELT.display-name", "Belt");
            config.set("slot-types.BELT.max-slots", 1);
            config.set("slot-types.BELT.required-type", "BELT");
            config.set("slot-types.GLOVES.display-name", "Gloves");
            config.set("slot-types.GLOVES.max-slots", 2);
            config.set("slot-types.GLOVES.required-type", "GLOVES");
            config.set("slot-types.RING.display-name", "Ring");
            config.set("slot-types.RING.max-slots", 2);
            config.set("slot-types.RING.required-type", "RING");
            config.set("slot-types.NECKLACE.display-name", "Necklace");
            config.set("slot-types.NECKLACE.max-slots", 1);
            config.set("slot-types.NECKLACE.required-type", "NECKLACE");
        }
    }
}
