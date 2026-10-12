package org.yuemi.mmoaccessories.plugin.command.subcommands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.yuemi.mmoaccessories.api.AccessoriesApi;
import org.yuemi.mmoaccessories.plugin.command.BaseSubCommand;

import java.util.List;
import java.util.stream.Collectors;

public final class ViewCommand extends BaseSubCommand {

    private final AccessoriesApi api;

    public ViewCommand(@NotNull AccessoriesApi api) {
        this.api = api;
    }

    @Override
    public @NotNull String getName() {
        return "view";
    }

    @Override
    public @NotNull String getDescription() {
        return "Open another player's accessories GUI";
    }

    @Override
    public @NotNull String getPermission() {
        return "accessories.command.view";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player viewer)) {
            sendMessage(sender, "<red>This command requires a player to open the GUI.</red>");
            return;
        }

        if (args.length < 1) {
            sendMessage(sender, "<red>Usage: /accessories view <player></red>");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sendMessage(sender, "<red>Player '" + args[0] + "' not found or not online.</red>");
            return;
        }

        if (!api.isAvailable()) {
            sendMessage(sender, "<red>Accessories system is not available.</red>");
            return;
        }

        // Reuse the same GUI but pass the target as the player
        // The GUI is player-keyed, so this opens the viewer's GUI, but
        // the equipped items shown are for the target.
        api.openGui(target);
        sendMessage(sender, "<green>Viewing " + target.getName() + "'s accessories.</green>");
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
