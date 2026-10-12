package org.yuemi.mmoaccessories.plugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.yuemi.mmoaccessories.api.AccessoriesApi;
import org.yuemi.mmoaccessories.api.AccessoriesApiProvider;
import org.yuemi.mmoaccessories.plugin.command.AccessoriesCommand;
import org.yuemi.mmoaccessories.plugin.config.SlotConfig;
import org.yuemi.mmoaccessories.plugin.gui.AccessoriesGui;
import org.yuemi.mmoaccessories.plugin.listener.AccessoryDragListener;
import org.yuemi.mmoaccessories.plugin.listener.AccessoryInteractionListener;

public class AccessoriesPlugin extends JavaPlugin {

    private SlotConfig slotConfig;
    private AccessoriesApiImpl api;
    private AccessoriesGui gui;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Check and migrate config
        new org.yuemi.config.api.ConfigManager(this, "org.yuemi.mmoaccessories.plugin.config.migration").loadAndMigrate(this);
        reloadConfig();

        // Load slot configuration
        this.slotConfig = new SlotConfig();
        this.slotConfig.loadFromConfig(getConfig());

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
        Object mmoMechanicsApi = getMmoMechanicsApi();
        if (mmoMechanicsApi != null) {
            getServer().getPluginManager().registerEvents(
                new AccessoryInteractionListener(api, getLogger(), mmoMechanicsApi),
                this
            );
            getLogger().info("Hooked into MmoMechanics for accessory skill triggering!");
        } else {
            getLogger().info("MmoMechanics not available - accessory mechanics disabled.");
        }

        // Register drag-and-drop listener for GUI
        if (gui != null) {
            getServer().getPluginManager().registerEvents(
                new AccessoryDragListener(api, gui),
                this
            );
        }

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

    public AccessoriesApiImpl getApi() {
        return api;
    }

    public AccessoriesGui getGui() {
        return gui;
    }

    private Object getMmoMechanicsApi() {
        try {
            Class<?> providerClass = Class.forName("org.yuemi.mmomechanics.api.MmoMechanicsApiProvider");
            Object provider = providerClass.getMethod("getApi").invoke(null);
            if (provider == null) {
                return null;
            }
            return providerClass.getMethod("getApi").invoke(provider);
        } catch (Exception e) {
            return null;
        }
    }
}
