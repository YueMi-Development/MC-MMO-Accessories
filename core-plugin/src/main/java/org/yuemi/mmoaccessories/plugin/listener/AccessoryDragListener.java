package org.yuemi.mmoaccessories.plugin.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.yuemi.mmoaccessories.api.AccessorySlot;
import org.yuemi.mmoaccessories.plugin.AccessoriesApiImpl;
import org.yuemi.mmoaccessories.plugin.gui.AccessoriesGui;

import java.util.Optional;

/**
 * Handles drag-and-drop interactions for the accessories GUI.
 * Players can drag items from inventory into slots to equip,
 * and drag equipped items out to unequip.
 */
public class AccessoryDragListener implements Listener {

    private final AccessoriesApiImpl api;
    private final AccessoriesGui gui;

    public AccessoryDragListener(AccessoriesApiImpl api, AccessoriesGui gui) {
        this.api = api;
        this.gui = gui;
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isAccessoriesGui(event.getView().getTopInventory())) return;

        // Check if any dragged slot is a configured accessory slot
        boolean affectsAccessorySlot = false;
        for (int slot : event.getRawSlots()) {
            if (gui.getSlotMappings().containsKey(slot)) {
                affectsAccessorySlot = true;
                break;
            }
        }
        if (!affectsAccessorySlot) return;

        event.setCancelled(true);
        ItemStack cursor = event.getOldCursor();
        if (cursor == null || cursor.getType().isAir()) return;

        for (int slot : event.getRawSlots()) {
            AccessoriesGui.SlotMapping mapping = gui.getSlotMappings().get(slot);
            if (mapping == null) continue;
            handleEquip(player, cursor, mapping);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isAccessoriesGui(event.getView().getTopInventory())) return;

        int rawSlot = event.getRawSlot();
        AccessoriesGui.SlotMapping mapping = gui.getSlotMappings().get(rawSlot);
        if (mapping == null) return;

        InventoryAction action = event.getAction();

        // PLACE actions: cursor → slot (equip)
        if (action == InventoryAction.PLACE_ALL
                || action == InventoryAction.PLACE_ONE
                || action == InventoryAction.PLACE_SOME) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                event.setCancelled(true);
                handleEquip(player, cursor, mapping);
                return;
            }
        }

        // SWAP with hotbar / MOVE_TO_OTHER_INVENTORY: take item from slot
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY
                || action == InventoryAction.PICKUP_ALL
                || action == InventoryAction.SWAP_WITH_CURSOR
                || (action != null && action.name().startsWith("HOTBAR_SWAP"))) {
            Optional<ItemStack> equipped = api.getEquipped(player, mapping.slot(), mapping.index());
            if (equipped.isPresent()) {
                event.setCancelled(true);
                ItemStack item = equipped.get();
                if (api.unequipAccessory(player, mapping.slot(), mapping.index())) {
                    if (player.getInventory().firstEmpty() != -1) {
                        player.getInventory().addItem(item);
                    } else {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    }
                    api.sendMessage(player, "<green>Unequipped!");
                }
            }
        }
    }

    private void handleEquip(Player player, ItemStack cursor, AccessoriesGui.SlotMapping mapping) {
        AccessorySlot slot = mapping.slot();
        if (!api.canEquipToSlot(cursor, slot)) {
            api.sendMessage(player, "<red>Cannot equip this item to " + slot.name() + "!");
            return;
        }

        if (api.equipAccessory(player, slot, mapping.index(), cursor.clone())) {
            cursor.setAmount(cursor.getAmount() - 1);
            api.sendMessage(player, "<green>Equipped to " + slot.name() + "!");
        } else {
            api.sendMessage(player, "<red>Failed to equip!");
        }
    }

    private boolean isAccessoriesGui(org.bukkit.inventory.Inventory inventory) {
        if (inventory == null || inventory.getHolder() == null) return false;
        return inventory.getHolder().getClass().getName().contains("GuiHolder");
    }
}
