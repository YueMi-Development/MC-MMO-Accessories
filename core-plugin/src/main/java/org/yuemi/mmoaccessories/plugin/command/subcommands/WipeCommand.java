package org.yuemi.mmoaccessories.plugin.command.subcommands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.yuemi.mmoaccessories.api.AccessoriesApi;
import org.yuemi.mmoaccessories.api.AccessorySlot;
import org.yuemi.mmoaccessories.plugin.command.BaseSubCommand;

import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code /accessories wipe <player>} — unequip every accessory the target
 * has equipped and return the items to the target's inventory (drops
 * straight to the world if the inventory is full).
 *
 * <p>Sender: any (player or console). Permission:
 * {@code accessories.command.wipe`. Args: {@code <player>} (online player).
 * Each removed accessory fires the {@code onUnequip} trigger through
 * MMO Mechanics.
 */
public final class WipeCommand extends BaseSubCommand {

    private final AccessoriesApi api;

    public WipeCommand(@NotNull AccessoriesApi api) {
        this.api = api;
    }

    @Override
    public @NotNull String getName() {
        return "wipe";
    }

    @Override
    public @NotNull String getDescription() {
        return "Wipe a player's accessories (drops to inventory or vanishes)";
    }

    @Override
    public @NotNull String getPermission() {
        return "accessories.command.wipe";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 1) {
            sendMessage(sender, "<red>Usage: /accessories wipe <player></red>");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sendMessage(sender, "<red>Player '" + args[0] + "' not found or not online.</red>");
            return;
        }

        int count = 0;
        for (AccessorySlot slot : AccessorySlot.values()) {
            for (int i = 0; i < api.getMaxSlots(slot); i++) {
                var current = api.getEquipped(target, slot, i);
                if (current.isPresent() && api.unequipAccessory(target, slot, i)) {
                    var leftover = current.get();
                    if (leftover.getAmount() > 0) {
                        target.getInventory().addItem(leftover);
                    }
                    count++;
                }
            }
        }
        sendMessage(sender, "<green>Wiped " + count + " accessories from " + target.getName() + ".</green>");
        if (sender != target) {
            sendMessage(target, "<yellow>Your accessories have been wiped by an administrator.</yellow>");
        }
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
