package org.yuemi.mmoaccessories.plugin.command;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class BaseSubCommand {

    protected final MiniMessage miniMessage = MiniMessage.miniMessage();

    @NotNull
    public abstract String getName();

    @NotNull
    public abstract String getDescription();

    @NotNull
    public abstract String getPermission();

    public abstract void execute(@NotNull CommandSender sender, @NotNull String[] args);

    @NotNull
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return List.of();
    }

    protected void sendMessage(@NotNull CommandSender sender, String message) {
        sender.sendMessage(miniMessage.deserialize(message));
    }
}
