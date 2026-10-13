package org.yuemi.mmoaccessories.plugin.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.yuemi.mmoaccessories.api.storage.AccessoryStorage;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SQLite file-based storage.
 * Stores accessory data in a local SQLite database file.
 */
public class SqliteStorage implements AccessoryStorage {

    private Plugin plugin;
    private Connection connection;
    private File databaseFile;

    @Override
    public void initialize(Plugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        String fileName = config.getString("storage.sqlite.file", "accessories.db");
        File dataFolder = plugin.getDataFolder();
        this.databaseFile = new File(dataFolder, fileName);

        try {
            if (!databaseFile.getParentFile().exists()) {
                databaseFile.getParentFile().mkdirs();
            }
            if (!databaseFile.exists()) {
                databaseFile.createNewFile();
            }
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS accessories (" +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "slot_index INT NOT NULL, " +
                    "item_id VARCHAR(255) NOT NULL, " +
                    "PRIMARY KEY (player_uuid, slot_index))"
                );
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize SQLite storage: " + e.getMessage());
        }
    }

    @Override
    public @NotNull List<String> loadAccessories(UUID playerUuid) {
        List<String> accessories = new ArrayList<>();
        if (connection == null) return accessories;

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT slot_index, item_id FROM accessories WHERE player_uuid = ? ORDER BY slot_index")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idx = rs.getInt("slot_index");
                    String itemId = rs.getString("item_id");
                    // Ensure list is large enough
                    while (accessories.size() <= idx) {
                        accessories.add("");
                    }
                    accessories.set(idx, itemId == null ? "" : itemId);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load accessories from SQLite: " + e.getMessage());
        }
        return accessories;
    }

    @Override
    public void saveAccessories(UUID playerUuid, @NotNull List<String> itemIds) {
        if (connection == null) return;
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM accessories WHERE player_uuid = ?")) {
                delete.setString(1, playerUuid.toString());
                delete.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO accessories (player_uuid, slot_index, item_id) VALUES (?, ?, ?)")) {
                for (int i = 0; i < itemIds.size(); i++) {
                    String itemId = itemIds.get(i);
                    if (itemId == null || itemId.isEmpty()) continue;
                    insert.setString(1, playerUuid.toString());
                    insert.setInt(2, i);
                    insert.setString(3, itemId);
                    insert.addBatch();
                }
                insert.executeBatch();
            }
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save accessories to SQLite: " + e.getMessage());
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {}
        }
    }

    @Override
    public void clearAccessories(UUID playerUuid) {
        if (connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM accessories WHERE player_uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to clear accessories in SQLite: " + e.getMessage());
        }
    }

    @Override
    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {}
        connection = null;
    }

    @Override
    public @NotNull String getTypeName() {
        return "sqlite";
    }
}
