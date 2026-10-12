package org.yuemi.mmoaccessories.plugin.command.subcommands;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.yuemi.mmoaccessories.plugin.AccessoriesPlugin;
import org.yuemi.mmoaccessories.plugin.command.BaseSubCommand;

import java.util.List;

public final class ReloadCommand extends BaseSubCommand {

    private final AccessoriesPlugin plugin;

    public ReloadCommand(@NotNull AccessoriesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "reload";
    }

    @Override
    public @NotNull String getDescription() {
        return "Reload the plugin configuration";
    }

    @Override
    public @NotNull String getPermission() {
        return "accessories.command.reload";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        try {
            plugin.reloadConfig();
            plugin.getSlotConfig().loadFromConfig(plugin.getConfig());
            plugin.getApi().reload();
            sendMessage(sender, "<green>Configuration reloaded successfully!</green>");
        } catch (Exception e) {
            sendMessage(sender, "<red>Failed to reload configuration: " + e.getMessage() + "</red>");
            e.printStackTrace();
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return List.of();
    }
}
