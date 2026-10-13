package org.yuemi.mmoaccessories.plugin.storage;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.yuemi.mmoaccessories.api.storage.AccessoryStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Player {@code PersistentDataContainer} storage.
 *
 * <p>Default storage backend. Stores a single pipe-delimited string of
 * accessory item ids under the {@code accessories:accessories}
 * {@link NamespacedKey} in each player's PDC. No schema upgrades beyond
 * re-selecting the storage type in {@code config.yml}.
 *
 * <p>Limitations: only the currently-online player can be read or
 * written, since Bukkit only exposes a player's PDC while they're
 * connected. Cross-server scenarios need a database backend.
 */
public class PdcStorage implements AccessoryStorage {

    private Plugin plugin;
    private NamespacedKey accessoriesKey;

    /**
     * Initialise the backend. Reads {@code storage.sqlite.file} (default
     * {@code accessories.db}) and creates the database file and
     * {@code accessories} table if they don't already exist.
     */
    @Override
    public void initialize(Plugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.accessoriesKey = new NamespacedKey(plugin, "accessories");
    }

    /**
     * @return the per-player item-id list read from PDC, in slot order
     */
    @Override
    public @NotNull List<String> loadAccessories(UUID playerUuid) {
        Player player = getPlayer(playerUuid);
        if (player == null) return new ArrayList<>();

        List<String> accessories = new ArrayList<>();
        String stored = player.getPersistentDataContainer()
                .get(accessoriesKey, PersistentDataType.STRING);
        if (stored != null && !stored.isEmpty()) {
            for (String part : stored.split("\\|")) {
                accessories.add(part.isEmpty() ? "" : part);
            }
        }
        return accessories;
    }

    /**
     * Replace the player's record with {@code itemIds}. Empty strings
     * (representing empty slots) are preserved; the resulting PDC string
     * is {@code id0|id1|...|idN}.
     */
    @Override
    public void saveAccessories(UUID playerUuid, @NotNull List<String> itemIds) {
        Player player = getPlayer(playerUuid);
        if (player == null) return;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < itemIds.size(); i++) {
            if (i > 0) sb.append("|");
            sb.append(itemIds.get(i));
        }
        player.getPersistentDataContainer().set(accessoriesKey, PersistentDataType.STRING, sb.toString());
    }

    /**
     * Remove the {@code accessories:accessories} key from the player's PDC.
     */
    @Override
    public void clearAccessories(UUID playerUuid) {
        Player player = getPlayer(playerUuid);
        if (player == null) return;
        player.getPersistentDataContainer().remove(accessoriesKey);
    }

    /** No-op: PDC needs no teardown. */
    @Override
    public void shutdown() {
        // No-op
    }

    @Override
    public @NotNull String getTypeName() {
        return "pdc";
    }

    private Player getPlayer(UUID uuid) {
        if (plugin == null) return null;
        return plugin.getServer().getPlayer(uuid);
    }
}
