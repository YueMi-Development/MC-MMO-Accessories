package org.yuemi.mmoaccessories.plugin.command.subcommands;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.yuemi.mmoaccessories.api.AccessoriesApi;
import org.yuemi.mmoaccessories.api.AccessorySlot;
import org.yuemi.mmoaccessories.plugin.command.BaseSubCommand;

import java.util.List;
import java.util.stream.Stream;

public final class EquipCommand extends BaseSubCommand {

    private final AccessoriesApi api;

    public EquipCommand(@NotNull AccessoriesApi api) {
        this.api = api;
    }

    @Override
    public @NotNull String getName() {
        return "equip";
    }

    @Override
    public @NotNull String getDescription() {
        return "Equip item from main hand to a slot";
    }

    @Override
    public @NotNull String getPermission() {
        return "accessories.command.equip";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, "<red>This command requires a player.</red>");
            return;
        }

        if (args.length < 1) {
            sendMessage(sender, "<red>Usage: /accessories equip <slot> [index]</red>");
            return;
        }

        AccessorySlot slot = parseSlot(args[0]);
        if (slot == null) {
            sendMessage(sender, "<red>Invalid slot! Valid: BELT, GLOVES, RING, NECKLACE</red>");
            return;
        }

        int index = 0;
        if (args.length >= 2) {
            try {
                index = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sendMessage(sender, "<red>Invalid index!</red>");
                return;
            }
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            sendMessage(sender, "<red>Hold the item you want to equip in your main hand!</red>");
            return;
        }

        if (api.equipAccessory(player, slot, index, hand)) {
            hand.setAmount(0);
            sendMessage(sender, "<green>Equipped to " + slot.name() + " slot " + index + "!</green>");
        } else {
            sendMessage(sender, "<red>Cannot equip this item to that slot!</red>");
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            return Stream.of("BELT", "GLOVES", "RING", "NECKLACE")
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        } else if (args.length == 2) {
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
}
