package org.yuemi.mmoaccessories.plugin;

import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.yuemi.mmoaccessories.api.AccessoriesApi;
import org.yuemi.mmoaccessories.api.AccessoriesApiProvider;
import org.yuemi.mmoaccessories.plugin.config.SlotConfig;

public class AccessoriesPlugin extends JavaPlugin {

    private SlotConfig slotConfig;
    private AccessoriesApiImpl api;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        // Check and migrate config
        new org.yuemi.config.api.ConfigManager(this, "org.yuemi.mmoaccessories.plugin.config.migration").loadAndMigrate(this);
        reloadConfig();

        // Load slot configuration
        this.slotConfig = new SlotConfig();
        this.slotConfig.loadFromConfig(getConfig());

        // Initialize API
        this.api = new AccessoriesApiImpl(this, slotConfig);

        // Register API
        getServer().getServicesManager().register(
                AccessoriesApi.class,
                api,
                this,
                ServicePriority.Normal
        );

        AccessoriesApiProvider.setApi(api);

        getLogger().info("MmoAccessories enabled!");
    }

    @Override
    public void onDisable() {
        AccessoriesApiProvider.setApi(null);
        getServer().getServicesManager().unregister(AccessoriesApi.class, api);
        getLogger().info("MmoAccessories disabled!");
    }

    public SlotConfig getSlotConfig() {
        return slotConfig;
    }
}
