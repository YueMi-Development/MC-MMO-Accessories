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
 * MySQL/MariaDB connection pool storage.
 * Uses HikariCP for connection pooling.
 */
public class MySqlStorage implements AccessoryStorage {

    private Plugin plugin;
    private HikariDataSource dataSource;

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
