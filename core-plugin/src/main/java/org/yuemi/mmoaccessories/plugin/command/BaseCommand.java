package org.yuemi.mmoaccessories.plugin.command;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class BaseCommand implements CommandExecutor, TabCompleter {

    protected final Map<String, BaseSubCommand> subCommands = new LinkedHashMap<>();
    protected final MiniMessage miniMessage = MiniMessage.miniMessage();

    protected void register(@NotNull BaseSubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subName = args[0].toLowerCase();
        BaseSubCommand subCommand = subCommands.get(subName);

        if (subCommand == null) {
            sendHelp(sender);
            return true;
        }

        if (!sender.hasPermission(subCommand.getPermission())) {
            sendMessage(sender, "<red>You do not have permission to execute this command.</red>");
            return true;
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        subCommand.execute(sender, subArgs);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            return subCommands.values().stream()
                    .filter(sub -> sender.hasPermission(sub.getPermission()))
                    .map(BaseSubCommand::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length > 1) {
            String subName = args[0].toLowerCase();
            BaseSubCommand subCommand = subCommands.get(subName);
            if (subCommand != null && sender.hasPermission(subCommand.getPermission())) {
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                return subCommand.tabComplete(sender, subArgs);
            }
        }
        return Collections.emptyList();
    }

    protected void sendHelp(CommandSender sender) {
        sendMessage(sender, "<gold>=== " + getCommandLabel() + " Commands ===</gold>");
        for (BaseSubCommand sub : subCommands.values()) {
            if (sender.hasPermission(sub.getPermission())) {
                sendMessage(sender, "<yellow>/" + getCommandLabel().toLowerCase() + " " + sub.getName() + "</yellow> - " + sub.getDescription());
            }
        }
    }

    protected void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(miniMessage.deserialize(message));
    }

    protected abstract String getCommandLabel();
}
