package org.yuemi.mmoaccessories.plugin.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Configuration for the global accessories GUI (title, rows, action buttons, etc.)
 */
public class GuiConfig {

    private final String title;
    private final int rows;
    private final boolean backgroundEnabled;
    private final String backgroundIcon;
    private final ActionButton close;

    public GuiConfig(
        String title, int rows,
        boolean backgroundEnabled, String backgroundIcon,
        ActionButton close
    ) {
        this.title = title;
        this.rows = rows;
        this.backgroundEnabled = backgroundEnabled;
        this.backgroundIcon = backgroundIcon;
        this.close = close;
    }

    public static GuiConfig loadFromConfig(FileConfiguration config) {
        return new GuiConfig(
            config.getString("gui.title", "Accessories"),
            config.getInt("gui.rows", 6),
            config.getBoolean("gui.background.enabled", true),
            config.getString("gui.background.icon", "minecraft:black_stained_glass_pane"),
            ActionButton.fromConfig(config, "gui.action-buttons.close", 53,
                "minecraft:arrow", "<red><bold>Close</bold></red>",
                List.of("<gray>Close this menu</gray>"))
        );
    }

    public String getTitle() { return title; }
    public int getRows() { return rows; }
    public boolean isBackgroundEnabled() { return backgroundEnabled; }
    public String getBackgroundIcon() { return backgroundIcon; }
    public ActionButton getClose() { return close; }

    /**
     * A configurable action button in the GUI.
     */
    public record ActionButton(int slot, @NotNull String icon, @NotNull String name, @NotNull List<String> lore) {
        public static ActionButton fromConfig(FileConfiguration config, String path, int defaultSlot,
                                              String defaultIcon, String defaultName, List<String> defaultLore) {
            int slot = config.getInt(path + ".slot", defaultSlot);
            String icon = config.getString(path + ".icon", defaultIcon);
            String name = config.getString(path + ".name", defaultName);
            List<String> lore = config.getStringList(path + ".lore");
            if (lore == null) lore = defaultLore;
            return new ActionButton(slot, icon, name, lore);
        }
    }
}
