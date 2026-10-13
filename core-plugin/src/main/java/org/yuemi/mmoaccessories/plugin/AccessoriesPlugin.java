package org.yuemi.mmoaccessories.plugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.yuemi.mmomechanics.api.MmoMechanicsApi;
import org.yuemi.mmoaccessories.api.AccessoriesApi;
import org.yuemi.mmoaccessories.api.AccessoriesApiProvider;
import org.yuemi.mmoaccessories.api.storage.AccessoryStorage;
import org.yuemi.mmoaccessories.api.storage.AccessoryStorageFactory;
import org.yuemi.mmoaccessories.plugin.command.AccessoriesCommand;
import org.yuemi.mmoaccessories.plugin.config.SlotConfig;
import org.yuemi.mmoaccessories.plugin.gui.AccessoriesGui;
import org.yuemi.mmoaccessories.plugin.listener.AccessoryInteractionListener;

public class AccessoriesPlugin extends JavaPlugin {

    private SlotConfig slotConfig;
    private AccessoriesApiImpl api;
    private AccessoriesGui gui;
    private AccessoryStorage storage;

    private static volatile AccessoriesPlugin instance;

    public static AccessoriesPlugin getInstance() {
        return instance;
    }

    public AccessoryStorage getStorage() {
        return storage;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Check and migrate config
        new org.yuemi.config.api.ConfigManager(this, "org.yuemi.mmoaccessories.plugin.config.migration").loadAndMigrate(this);
        reloadConfig();

        // Load slot configuration
        this.slotConfig = new SlotConfig();
        this.slotConfig.loadFromConfig(getConfig());

        // Initialize storage backend
        String storageType = getConfig().getString("storage.type", "pdc");
        this.storage = AccessoryStorageFactory.create(storageType);
        try {
            this.storage.initialize(this, getConfig());
            getLogger().info("Storage initialized: " + storage.getTypeName());
        } catch (Exception e) {
            getLogger().severe("Failed to initialize " + storageType + " storage: " + e.getMessage());
            getLogger().warning("Falling back to PDC storage");
            this.storage = AccessoryStorageFactory.createPdc();
            try {
                this.storage.initialize(this, getConfig());
            } catch (Exception ex) {
                getLogger().severe("PDC storage also failed: " + ex.getMessage());
            }
        }

        // Initialize API and GUI
        this.api = new AccessoriesApiImpl(this, slotConfig);
        this.api.init();
        this.gui = api.getGuiConfig() != null ? new AccessoriesGui(api, slotConfig, api.getGuiConfig()) : null;

        // Register API
        getServer().getServicesManager().register(
                AccessoriesApi.class,
                api,
                this,
                ServicePriority.Normal
        );

        AccessoriesApiProvider.setApi(api);

        // Register command
        AccessoriesCommand command = new AccessoriesCommand(api, this);
        var accessoriesCmd = getCommand("accessories");
        if (accessoriesCmd != null) {
            accessoriesCmd.setExecutor(command);
            accessoriesCmd.setTabCompleter(command);
        }

        // Register interaction listener
        MmoMechanicsApi mechanicsApi = Bukkit.getServicesManager().load(MmoMechanicsApi.class);
        if (mechanicsApi != null) {
            getServer().getPluginManager().registerEvents(
                new AccessoryInteractionListener(api, getLogger(), mechanicsApi),
                this
            );
            getLogger().info("Hooked into MmoMechanics for accessory skill triggering!");
        } else {
            getLogger().info("MmoMechanics not available - accessory mechanics disabled.");
        }

        getLogger().info("MmoAccessories enabled!");
    }

    @Override
    public void onDisable() {
        if (storage != null) {
            storage.shutdown();
        }
        AccessoriesApiProvider.setApi(null);
        getServer().getServicesManager().unregister(AccessoriesApi.class, api);
        getLogger().info("MmoAccessories disabled!");
    }

    public SlotConfig getSlotConfig() {
        return slotConfig;
    }

    public AccessoriesApiImpl getApi() {
        return api;
    }

    public AccessoriesGui getGui() {
        return gui;
    }
}
