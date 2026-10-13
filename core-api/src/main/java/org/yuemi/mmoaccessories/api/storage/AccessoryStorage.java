package org.yuemi.mmoaccessories.api.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Pluggable storage backend for accessory data.
 * Implementations can store accessory data in different ways
 * (PDC, SQLite, MySQL, MongoDB, etc.) by implementing this interface.
 */
public interface AccessoryStorage {

    /**
     * Initialize the storage backend. Called once when the plugin enables.
     *
     * @param plugin the plugin instance
     * @param config the plugin's FileConfiguration
     */
    void initialize(Plugin plugin, FileConfiguration config);

    /**
     * Load the list of equipped item IDs for a player.
     * The list is in slot order (index = slot position).
     * Returns an empty list if the player has no saved accessories.
     *
     * @param playerUuid the player's UUID
     * @return the list of item IDs (in slot order), empty if none
     */
    @NotNull List<String> loadAccessories(UUID playerUuid);

    /**
     * Save the list of equipped item IDs for a player.
     *
     * @param playerUuid the player's UUID
     * @param itemIds the list of item IDs (in slot order)
     */
    void saveAccessories(UUID playerUuid, @NotNull List<String> itemIds);

    /**
     * Clear all equipped accessories for a player.
     *
     * @param playerUuid the player's UUID
     */
    void clearAccessories(UUID playerUuid);

    /**
     * Shutdown the storage backend. Called when the plugin disables.
     * Should close any open connections.
     */
    void shutdown();

    /**
     * Get the type name of this storage backend.
     * Used for logging and config identification.
     *
     * @return the storage type name
     */
    @NotNull String getTypeName();
}
