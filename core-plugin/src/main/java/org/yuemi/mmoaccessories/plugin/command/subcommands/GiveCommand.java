package org.yuemi.mmoaccessories.plugin.command.subcommands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.yuemi.mmoaccessories.plugin.command.BaseSubCommand;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

public final class GiveCommand extends BaseSubCommand {

    public GiveCommand() {
    }

    @Override
    public @NotNull String getName() {
        return "give";
    }

    @Override
    public @NotNull String getDescription() {
        return "Give a custom item to a player";
    }

    @Override
    public @NotNull String getPermission() {
        return "accessories.command.give";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sendMessage(sender, "<red>Usage: /accessories give <player> <itemId> [amount]</red>");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sendMessage(sender, "<red>Player '" + args[0] + "' not found or not online.</red>");
            return;
        }

        String itemId = args[1];
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {
            }
        }
        if (amount < 1) amount = 1;

        ItemStack item = createCustomItem(itemId);
        if (item == null) {
            sendMessage(sender, "<red>Could not resolve item '" + itemId + "'. Is MmoItems installed?</red>");
            return;
        }
        item.setAmount(amount);
        target.getInventory().addItem(item);
        sendMessage(sender, "<green>Gave " + amount + "x '" + itemId + "' to " + target.getName() + ".</green>");
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 3) {
            return List.of("1", "8", "16", "32", "64");
        }
        return List.of();
    }

    private ItemStack createCustomItem(String itemId) {
        try {
            Class<?> apiClass = Class.forName("org.yuemi.mmoitems.api.MmoItemsApi");
            Class<?> providerClass = Class.forName("org.yuemi.mmoitems.api.MmoItemsApiProvider");
            Object provider = providerClass.getMethod("getApi").invoke(null);
            if (provider == null) return null;
            return (ItemStack) apiClass.getMethod("createCustomItem", String.class).invoke(provider, itemId);
        } catch (Exception e) {
            return null;
        }
    }
}
