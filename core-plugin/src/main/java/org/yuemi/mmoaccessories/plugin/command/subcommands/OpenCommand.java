package org.yuemi.mmoaccessories.plugin.command.subcommands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.yuemi.mmoaccessories.api.AccessoriesApi;
import org.yuemi.mmoaccessories.plugin.command.BaseSubCommand;

import java.util.List;

public final class OpenCommand extends BaseSubCommand {

    private final AccessoriesApi api;

    public OpenCommand(@NotNull AccessoriesApi api) {
        this.api = api;
    }

    @Override
    public @NotNull String getName() {
        return "open";
    }

    @Override
    public @NotNull String getDescription() {
        return "Open your accessories GUI";
    }

    @Override
    public @NotNull String getPermission() {
        return "accessories.command.open";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, "<red>This command requires a player.</red>");
            return;
        }
        if (!api.isAvailable()) {
            sendMessage(sender, "<red>Accessories system is not available.</red>");
            return;
        }
        api.openGui(player);
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return List.of();
    }
}
