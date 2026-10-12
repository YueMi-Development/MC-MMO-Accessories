package org.yuemi.mmoaccessories.plugin.command.subcommands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.yuemi.mmoaccessories.api.AccessoriesApi;
import org.yuemi.mmoaccessories.api.AccessorySlot;
import org.yuemi.mmoaccessories.plugin.command.BaseSubCommand;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SetCommand extends BaseSubCommand {

    private final AccessoriesApi api;

    public SetCommand(@NotNull AccessoriesApi api) {
        this.api = api;
    }

    @Override
    public @NotNull String getName() {
        return "set";
    }

    @Override
    public @NotNull String getDescription() {
        return "Set a slot for a player using an itemId (mmoitems)";
    }

    @Override
    public @NotNull String getPermission() {
        return "accessories.command.set";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 4) {
            sendMessage(sender, "<red>Usage: /accessories set <player> <slot> <index> <itemId></red>");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sendMessage(sender, "<red>Player '" + args[0] + "' not found or not online.</red>");
            return;
        }

        AccessorySlot slot = parseSlot(args[1]);
        if (slot == null) {
            sendMessage(sender, "<red>Invalid slot! Valid: BELT, GLOVES, RING, NECKLACE</red>");
            return;
        }

        int index;
        try {
            index = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sendMessage(sender, "<red>Invalid index!</red>");
            return;
        }

        String itemId = args[3];
        ItemStack item = createItemFromId(itemId);
        if (item == null) {
            sendMessage(sender, "<red>Could not resolve item '" + itemId + "'. Is MmoItems installed?</red>");
            return;
        }

        if (api.equipAccessory(target, slot, index, item)) {
            sendMessage(sender, "<green>Set " + slot.name() + "[" + index + "] on " + target.getName() + " to '" + itemId + "'.</green>");
        } else {
            sendMessage(sender, "<red>Failed to set slot (invalid slot, wrong type, or index out of range).</red>");
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            return Stream.of("BELT", "GLOVES", "RING", "NECKLACE")
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        } else if (args.length == 3) {
            return List.of("0", "1");
        }
        return List.of();
    }

    private AccessorySlot parseSlot(String name) {
        try {
            return AccessorySlot.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private ItemStack createItemFromId(String itemId) {
        try {
            Class<?> apiClass = Class.forName("org.yuemi.mmoitems.api.MmoItemsApi");
            Class<?> providerClass = Class.forName("org.yuemi.mmoitems.api.MmoItemsApiProvider");
            Method getApi = providerClass.getMethod("getApi");
            Object provider = getApi.invoke(null);
            if (provider == null) return null;
            return (ItemStack) apiClass.getMethod("createCustomItem", String.class).invoke(provider, itemId);
        } catch (Exception e) {
            return null;
        }
    }
}
