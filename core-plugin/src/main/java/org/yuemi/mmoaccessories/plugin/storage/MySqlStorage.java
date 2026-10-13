package org.yuemi.mmoaccessories.plugin.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.yuemi.mmoaccessories.api.storage.AccessoryStorage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * MySQL / MariaDB storage backed by HikariCP.
 *
 * <p>Configuration under {@code storage.mysql.*}:
 * <ul>
 *   <li>{@code host} (default {@code localhost})</li>
 *   <li>{@code port} (default {@code 3306})</li>
 *   <li>{@code database} (default {@code minecraft})</li>
 *   <li>{@code username} / {@code password}</li>
 *   <li>{@code pool-size} (default {@code 10})</li>
 * </ul>
 *
 * <p>One row per (player_uuid, slot_index). Reads and writes go through
 * a HikariCP pool (named {@code MmoAccessories-MySQL}). The save path
 * uses an explicit transaction (delete-all + batched insert) so the
 * per-player state is atomic.
 */
public class MySqlStorage implements AccessoryStorage {

    private Plugin plugin;
    private HikariDataSource dataSource;

    /**
     * Build the Hikari config from {@code storage.mysql.*} and create
     * the {@code accessories} table if it doesn't already exist. The
     * pool is named {@code MmoAccessories-MySQL}.
     */
    @Override
    public void initialize(Plugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        String host = config.getString("storage.mysql.host", "localhost");
        int port = config.getInt("storage.mysql.port", 3306);
        String database = config.getString("storage.mysql.database", "minecraft");
        String username = config.getString("storage.mysql.username", "root");
        String password = config.getString("storage.mysql.password", "");
        int poolSize = config.getInt("storage.mysql.pool-size", 10);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(poolSize);
        hikariConfig.setPoolName("MmoAccessories-MySQL");

        try {
            dataSource = new HikariDataSource(hikariConfig);
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS accessories (" +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "slot_index INT NOT NULL, " +
                    "item_id VARCHAR(255) NOT NULL, " +
                    "PRIMARY KEY (player_uuid, slot_index))"
                );
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize MySQL storage: " + e.getMessage());
        }
    }

    /** @return the per-player item-id list, in slot order, with empty slots filled in as empty strings. */
    @Override
    public @NotNull List<String> loadAccessories(UUID playerUuid) {
        List<String> accessories = new ArrayList<>();
        if (dataSource == null) return accessories;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT slot_index, item_id FROM accessories WHERE player_uuid = ? ORDER BY slot_index")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idx = rs.getInt("slot_index");
                    String itemId = rs.getString("item_id");
                    while (accessories.size() <= idx) {
                        accessories.add("");
                    }
                    accessories.set(idx, itemId == null ? "" : itemId);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load accessories from MySQL: " + e.getMessage());
        }
        return accessories;
    }

    /**
     * Replace the player's row set with the given item-id list. Wrapped
     * in a transaction; existing rows are deleted first, then the new
     * rows are inserted via a JDBC batch. Empty strings (representing
     * empty slots) are skipped.
     */
    @Override
    public void saveAccessories(UUID playerUuid, @NotNull List<String> itemIds) {
        if (dataSource == null) return;
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement delete = conn.prepareStatement(
                    "DELETE FROM accessories WHERE player_uuid = ?")) {
                delete.setString(1, playerUuid.toString());
                delete.executeUpdate();
            }
            try (PreparedStatement insert = conn.prepareStatement(
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
            conn.commit();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save accessories to MySQL: " + e.getMessage());
        }
    }

    /**
     * Delete every row for the player.
     */
    @Override
    public void clearAccessories(UUID playerUuid) {
        if (dataSource == null) return;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM accessories WHERE player_uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to clear accessories in MySQL: " + e.getMessage());
        }
    }

    /**
     * Close the HikariCP pool. Idempotent.
     */
    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        dataSource = null;
    }

    @Override
    public @NotNull String getTypeName() {
        return "mysql";
    }
}
