package org.yuemi.mmoaccessories.api.storage;

import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating AccessoryStorage instances.
 * Supports PDC (default), SQLite, MySQL, and MongoDB.
 * New storage types can be added by:
 * - Implementing AccessoryStorage interface
 * - Registering the implementation as a Bukkit service before
 *   MmoAccessories enables
 * - Or modifying this factory
 */
public final class AccessoryStorageFactory {

    private AccessoryStorageFactory() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Create a storage instance by type name.
     * The returned instance must be initialized before use.
     *
     * @param type the storage type ("pdc", "sqlite", "mysql", "mongodb")
     * @return the storage instance, defaults to PdcStorage if type is unknown
     */
    @NotNull
    public static AccessoryStorage create(@NotNull String type) {
        return switch (type.toLowerCase()) {
            case "sqlite" -> createSqlite();
            case "mysql" -> createMysql();
            case "mongodb", "mongo" -> createMongodb();
            default -> createPdc();
        };
    }

    /**
     * Create a PDC storage instance.
     * Implementation is loaded reflectively from the core-plugin to avoid
     * hard dependencies.
     */
    @NotNull
    public static AccessoryStorage createPdc() {
        return createByClassName("org.yuemi.mmoaccessories.plugin.storage.PdcStorage");
    }

    @NotNull
    public static AccessoryStorage createSqlite() {
        return createByClassName("org.yuemi.mmoaccessories.plugin.storage.SqliteStorage");
    }

    @NotNull
    public static AccessoryStorage createMysql() {
        return createByClassName("org.yuemi.mmoaccessories.plugin.storage.MySqlStorage");
    }

    @NotNull
    public static AccessoryStorage createMongodb() {
        return createByClassName("org.yuemi.mmoaccessories.plugin.storage.MongoStorage");
    }

    /**
     * Create a storage instance by fully qualified class name.
     * Returns PdcStorage as fallback if the class cannot be loaded.
     */
    @NotNull
    private static AccessoryStorage createByClassName(@NotNull String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return (AccessoryStorage) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            // Fallback to PDC if the specific storage class is unavailable
            try {
                Class<?> pdcClass = Class.forName("org.yuemi.mmoaccessories.plugin.storage.PdcStorage");
                return (AccessoryStorage) pdcClass.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                throw new IllegalStateException("Could not create any storage backend", ex);
            }
        }
    }
}
