package org.yuemi.mmoaccessories.plugin.config.migration;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.yuemi.config.api.MigrationStep;

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
