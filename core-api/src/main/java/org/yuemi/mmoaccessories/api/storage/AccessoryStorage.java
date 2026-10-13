package org.yuemi.mmoaccessories.api.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Pluggable storage backend for accessory data.
 *
 * <p>Implementations store one record per player, keyed by the player's
 * {@link UUID}. The record is a list of MMO Items item ids in slot order
 * (with empty slots represented as the empty string). The list length is
 * always equal to the sum of all slot types' {@code max-slots}, with
 * empty slots filled in as empty strings. Backends are free to compress
 * the list (e.g. drop trailing empty strings) on save and re-hydrate on
 * load.
 *
 * <p>Shipped backends: {@code pdc} (default), {@code sqlite}, {@code mysql},
 * {@code mongodb}. Add a new backend by implementing this interface and
 * resolving it through
 * {@link AccessoryStorageFactory#create(String)} (or wiring a new case
 * onto {@code AccessoryStorageFactory.create}, and registering the
 * implementation in your build).
 */
public interface AccessoryStorage {

    /**
     * Initialise the backend.
     *
     * <p>Called once when the plugin enables (after the storage type has
     * been resolved from {@code config.yml}). Implementations read their
     * own config block (e.g. {@code storage.sqlite.*},
     * {@code storage.mysql.*}, {@code storage.mongodb.*}) and prepare any
     * connection pools.
     *
     * @param plugin the plugin instance
     * @param config the plugin's {@code config.yml}
     */
    void initialize(Plugin plugin, FileConfiguration config);

    /**
     * Load the list of equipped item ids for a player.
     *
     * <p>The returned list is in slot order:
     * <pre>
     *   index 0        = BELT[0]
     *   index 1..2     = GLOVES[0..1]
     *   index 3..4     = RING[0..1]
     *   index 5        = NECKLACE[0]
     * </pre>
     * Empty slots are represented as the empty string.
     *
     * @param playerUuid the player's UUID
     * @return the item-id list, never {@code null} (empty if the player
     *         has no saved accessories)
     */
    @NotNull List<String> loadAccessories(UUID playerUuid);

    /**
     * Save the list of equipped item ids for a player. Replaces any
     * existing record.
     *
     * <p>Empty strings in the list represent empty slots; trailing empties
     * are accepted but the storage may compress them.
     *
     * @param playerUuid the player's UUID
     * @param itemIds    the item-id list (in slot order)
     */
    void saveAccessories(UUID playerUuid, @NotNull List<String> itemIds);

    /**
     * Remove the player's record entirely. Used by the {@code /wipe}
     * command and on player data reset.
     *
     * @param playerUuid the player's UUID
     */
    void clearAccessories(UUID playerUuid);

    /**
     * Tear down the backend. Called when the plugin disables or when the
     * storage type is changed at runtime. Implementations must close any
     * open connections, file handles, and connection pools.
     */
    void shutdown();

    /**
     * @return the storage type name ({@code "pdc"}, {@code "sqlite"},
     *         {@code "mysql"}, {@code "mongodb"}, or a custom name).
     *         Used for logging and {@code config.yml} identification.
     */
    @NotNull String getTypeName();
}
