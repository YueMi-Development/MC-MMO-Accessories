package org.yuemi.mmoaccessories.plugin.config.migration;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.yuemi.config.api.MigrationStep;

/**
 * Migrates config from v4 to v5: adds storage section.
 * Default storage is PDC (player PersistentDataContainer).
 */
public class MigrationStep4To5 implements MigrationStep {

    @Override
    public int getTargetVersion() {
        return 5;
    }

    /**
     * Introduce the {@code storage} block with all four backend
     * sub-sections (pdc, sqlite, mysql, mongodb) using their default
     * connection values. The {@code storage.type} is left at {@code pdc}.
     */
    @Override
    public void migrate(@NotNull FileConfiguration config) {
        if (!config.contains("storage")) {
            config.set("storage.type", "pdc");
            config.set("storage.sqlite.file", "accessories.db");
            config.set("storage.mysql.host", "localhost");
            config.set("storage.mysql.port", 3306);
            config.set("storage.mysql.database", "minecraft");
            config.set("storage.mysql.username", "root");
            config.set("storage.mysql.password", "");
            config.set("storage.mysql.pool-size", 10);
            config.set("storage.mongodb.uri", "mongodb://localhost:27017");
            config.set("storage.mongodb.database", "minecraft");
            config.set("storage.mongodb.collection", "accessories");
        }
    }
}
