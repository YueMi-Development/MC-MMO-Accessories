package org.yuemi.mmoaccessories.plugin.bstats;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * Initialises bStats metrics for MC-MMO-Accessories.
 *
 * <p>Reads {@code bstats-id} from the bundled {@code plugin.yml} and
 * registers a {@link Metrics} instance for the plugin. When the id is
 * unset (default empty string in {@code gradle.properties}) the service
 * silently no-ops.
 */
public final class BStatsService {

    private BStatsService() {
    }

    /**
     * Initialise bStats for {@code plugin} by reading the templated
     * {@code bstats-id} value from the bundled {@code plugin.yml}.
     *
     * @param plugin the plugin instance
     */
    public static void initialize(@NotNull JavaPlugin plugin) {
        try (InputStream input = plugin.getResource("plugin.yml")) {
            if (input == null) {
                return;
            }
            YamlConfiguration config = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(input, StandardCharsets.UTF_8));
            String rawId = config.getString("bstats-id");
            if (rawId == null || rawId.trim().isEmpty() || rawId.contains("bstatsPluginId")) {
                return;
            }
            try {
                int pluginId = Integer.parseInt(rawId.trim());
                new Metrics(plugin, pluginId);
                plugin.getLogger().info("bStats metrics initialized successfully with ID: " + pluginId);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid bStats plugin ID: " + rawId);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize bStats metrics", e);
        }
    }
}
