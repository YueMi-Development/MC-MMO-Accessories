package org.yuemi.mmoaccessories.plugin.command.subcommands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.yuemi.mmoaccessories.api.AccessoriesApi;
import org.yuemi.mmoaccessories.api.AccessorySlot;
import org.yuemi.mmoaccessories.plugin.command.BaseSubCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code /accessories list <player>} — list the target's currently equipped
 * accessories with their slot and display name.
 *
 * <p>Sender: any (player or console). Permission:
 * {@code accessories.command.list}. Args: {@code <player>} (online player
 * name; not validated for offline players — only online targets resolve
 * their items).
 */
public final class ListCommand extends BaseSubCommand {

    private final AccessoriesApi api;

    public ListCommand(@NotNull AccessoriesApi api) {
        this.api = api;
    }

    @Override
    public @NotNull String getName() {
        return "list";
    }

    @Override
    public @NotNull String getDescription() {
        return "List a player's equipped accessories";
    }

    @Override
    public @NotNull String getPermission() {
        return "accessories.command.list";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 1) {
            sendMessage(sender, "<red>Usage: /accessories list <player></red>");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sendMessage(sender, "<red>Player '" + args[0] + "' not found or not online.</red>");
            return;
        }

        sendMessage(sender, "<gold>=== Accessories of " + target.getName() + " ===</gold>");
        boolean any = false;
        for (AccessorySlot slot : AccessorySlot.values()) {
            int max = api.getMaxSlots(slot);
            for (int i = 0; i < max; i++) {
                var equipped = api.getEquipped(target, slot, i);
                if (equipped.isPresent()) {
                    ItemStack item = equipped.get();
                    String name = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                            ? item.getItemMeta().getDisplayName()
                            : item.getType().name();
                    sendMessage(sender, "<yellow>" + slot.name() + " [" + i + "]</yellow> - " + name);
                    any = true;
                }
            }
        }
        if (!any) {
            sendMessage(sender, "<gray>(no accessories equipped)</gray>");
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
