package org.yuemi.mmoaccessories.plugin.config.migration;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.yuemi.config.api.MigrationStep;

import java.util.List;

/**
 * Migrates config from v3 to v4: converts `required-type` to `accepted-types` list.
 * Old format: required-type: "BELT"
 * New format: accepted-types: ["BELT"]
 */
public class MigrationStep3To4 implements MigrationStep {

    private static final List<String> SLOT_NAMES = List.of("BELT", "GLOVES", "RING", "NECKLACE");

    @Override
    public int getTargetVersion() {
        return 4;
    }

    /**
     * Convert each slot's {@code required-type} string to an
     * {@code accepted-types} list (single-element). Removes the now-obsolete
     * {@code required-type} key.
     */
    @Override
    public void migrate(@NotNull FileConfiguration config) {
        if (config.contains("slots")) {
            for (String name : config.getConfigurationSection("slots").getKeys(false)) {
                String path = "slots." + name;
                String requiredType = config.getString(path + ".required-type");
                if (requiredType != null && !requiredType.isEmpty()) {
                    config.set(path + ".accepted-types", List.of(requiredType));
                }
                config.set(path + ".required-type", null);
            }
        }
    }
}
