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
 * Player PersistentDataContainer storage.
 * Default storage backend - stores accessory data in player's PDC.
 */
public class PdcStorage implements AccessoryStorage {

    private Plugin plugin;
    private NamespacedKey accessoriesKey;

    @Override
    public void initialize(Plugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.accessoriesKey = new NamespacedKey(plugin, "accessories");
    }

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

    @Override
    public void clearAccessories(UUID playerUuid) {
        Player player = getPlayer(playerUuid);
        if (player == null) return;
        player.getPersistentDataContainer().remove(accessoriesKey);
    }

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
