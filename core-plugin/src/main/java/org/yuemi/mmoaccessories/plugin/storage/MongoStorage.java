package org.yuemi.mmoaccessories.plugin.storage;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.yuemi.mmoaccessories.api.storage.AccessoryStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * MongoDB storage.
 *
 * <p>Configuration under {@code storage.mongodb.*}:
 * <ul>
 *   <li>{@code uri} (default {@code mongodb://localhost:27017})</li>
 *   <li>{@code database} (default {@code minecraft})</li>
 *   <li>{@code collection} (default {@code accessories})</li>
 * </ul>
 *
 * <p>Each player is stored as a single document keyed by
 * {@code _id = playerUuid} with an {@code items} array of item ids in
 * slot order. Writes use an upsert replace.
 */
public class MongoStorage implements AccessoryStorage {

    private Plugin plugin;
    private MongoClient mongoClient;
    private MongoCollection<Document> collection;

    /**
     * Connect to the MongoDB instance at {@code storage.mongodb.uri} and
     * obtain a handle to the {@code storage.mongodb.collection}
     * collection inside the {@code storage.mongodb.database} database.
     */
    @Override
    public void initialize(Plugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        String uri = config.getString("storage.mongodb.uri", "mongodb://localhost:27017");
        String dbName = config.getString("storage.mongodb.database", "minecraft");
        String collName = config.getString("storage.mongodb.collection", "accessories");

        try {
            mongoClient = MongoClients.create(uri);
            MongoDatabase database = mongoClient.getDatabase(dbName);
            collection = database.getCollection(collName);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize MongoDB storage: " + e.getMessage());
        }
    }

    /** @return the {@code items} array from the player's document, in slot order, or empty if no record exists. */
    @Override
    public @NotNull List<String> loadAccessories(UUID playerUuid) {
        List<String> accessories = new ArrayList<>();
        if (collection == null) return accessories;

        Document doc = collection.find(Filters.eq("_id", playerUuid.toString())).first();
        if (doc == null) return accessories;

        @SuppressWarnings("unchecked")
        List<String> items = (List<String>) doc.get("items");
        if (items != null) {
            accessories.addAll(items);
        }
        return accessories;
    }

    /**
     * Replace the player's document with a new one containing
     * {@code itemIds} as the {@code items} array. Upsert: if the
     * document doesn't exist it's created.
     */
    @Override
    public void saveAccessories(UUID playerUuid, @NotNull List<String> itemIds) {
        if (collection == null) return;
        Document doc = new Document("_id", playerUuid.toString())
                .append("items", itemIds);
        collection.replaceOne(
                Filters.eq("_id", playerUuid.toString()),
                doc,
                new ReplaceOptions().upsert(true)
        );
    }

    /**
     * Delete the player's document.
     */
    @Override
    public void clearAccessories(UUID playerUuid) {
        if (collection == null) return;
        collection.deleteOne(Filters.eq("_id", playerUuid.toString()));
    }

    /**
     * Close the {@link MongoClient}. Idempotent.
     */
    @Override
    public void shutdown() {
        if (mongoClient != null) {
            mongoClient.close();
        }
        mongoClient = null;
        collection = null;
    }

    @Override
    public @NotNull String getTypeName() {
        return "mongodb";
    }
}
