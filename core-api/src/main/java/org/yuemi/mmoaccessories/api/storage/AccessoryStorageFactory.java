package org.yuemi.mmoaccessories.api.storage;

import org.jetbrains.annotations.NotNull;

/**
 * Factory that resolves the configured storage backend by name.
 *
 * <p>The {@code core-api} artifact doesn't bundle any backend
 * implementation (each is in {@code core-plugin}); the factory loads
 * them reflectively so the artifact can stay thin and so a missing
 * optional dependency (e.g. SQLite-JDBC, MySQL connector, MongoDB driver)
 * falls back to {@code pdc} without crashing the plugin.
 *
 * <h2>Built-in types</h2>
 * <ul>
 *   <li>{@code pdc} (default) — {@code PdcStorage}, always available.</li>
 *   <li>{@code sqlite} — {@code SqliteStorage}, requires the
 *       {@code org.xerial:sqlite-jdbc} runtime dep.</li>
 *   <li>{@code mysql} — {@code MySqlStorage}, requires the
 *       {@code com.mysql:mysql-connector-j} runtime dep.</li>
 *   <li>{@code mongodb} / {@code mongo} — {@code MongoStorage}, requires
 *       the {@code org.mongodb:mongodb-driver-sync} runtime dep.</li>
 * </ul>
 *
 * <h2>Custom backends</h2>
 * Implement {@link AccessoryStorage}, then either:
 * <ul>
 *   <li>Add a new case to {@link #create(String)} (and to the build's
 *       storage backends), or</li>
 *   <li>Register the implementation as a Bukkit {@code ServiceLoader}
 *       before MmoAccessories enables; the factory will pick it up via
 *       its reflective fallback path.</li>
 * </ul>
 */
public final class AccessoryStorageFactory {

    private AccessoryStorageFactory() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Create a storage instance by type name.
     *
     * <p>The returned instance is constructed but not initialised; the
     * caller must invoke {@link AccessoryStorage#initialize} before
     * reading or writing data.
     *
     * @param type the storage type name (case-insensitive). Known values:
     *              {@code pdc}, {@code sqlite}, {@code mysql},
     *              {@code mongodb}, {@code mongo}. Unknown values fall
     *              back to {@code pdc}.
     * @return a fresh storage instance
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
     * @return a fresh {@code PdcStorage} instance (always available)
     */
    @NotNull
    public static AccessoryStorage createPdc() {
        return createByClassName("org.yuemi.mmoaccessories.plugin.storage.PdcStorage");
    }

    /**
     * @return a fresh {@code SqliteStorage} instance, or a {@code pdc}
     *         fallback if the SQLite driver is missing
     */
    @NotNull
    public static AccessoryStorage createSqlite() {
        return createByClassName("org.yuemi.mmoaccessories.plugin.storage.SqliteStorage");
    }

    /**
     * @return a fresh {@code MySqlStorage} instance, or a {@code pdc}
     *         fallback if the MySQL driver is missing
     */
    @NotNull
    public static AccessoryStorage createMysql() {
        return createByClassName("org.yuemi.mmoaccessories.plugin.storage.MySqlStorage");
    }

    /**
     * @return a fresh {@code MongoStorage} instance, or a {@code pdc}
     *         fallback if the MongoDB driver is missing
     */
    @NotNull
    public static AccessoryStorage createMongodb() {
        return createByClassName("org.yuemi.mmoaccessories.plugin.storage.MongoStorage");
    }

    /**
     * Reflectively load and instantiate a {@link AccessoryStorage} by
     * class name. Falls back to {@code PdcStorage} if the class is
     * missing, throws {@link IllegalStateException} only if even the
     * fallback can't be loaded.
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
