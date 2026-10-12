package org.yuemi.mmoaccessories.plugin.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.yuemi.libs.api.gui.GuiApi;
import org.yuemi.libs.api.gui.GuiItem;
import org.yuemi.libs.api.gui.GuiProvider;
import org.yuemi.mmoaccessories.api.AccessoriesApi;
import org.yuemi.mmoaccessories.api.AccessorySlot;
import org.yuemi.mmoaccessories.api.SlotDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * GUI for managing player accessories.
 */
public final class AccessoriesGui {

    private static final String GUI_TITLE = "<gold><bold>Accessories</bold></gold>";
    private static final int[] SLOT_OFFSETS = { 0, 1, 3, 5 }; // BELT, GLOVES, RING, NECKLACE

    private final AccessoriesApi api;

    public AccessoriesGui(AccessoriesApi api) {
        this.api = api;
    }

    public void open(Player player) {
        GuiApi guiApi = GuiProvider.getApi();
        if (guiApi == null) {
            api.sendMessage(player, "<red>YueMiLibs GUI is not available!");
            return;
        }

        guiApi.createBuilder()
                .title(GUI_TITLE)
                .rows(6)
                .createLayer("background", 0, layer -> layer.fill(createBackgroundItem()))
                .createLayer("slots", 1, layer -> buildSlotLayer(player, layer))
                .createLayer("actions", 2, layer -> buildActionLayer(player, layer))
                .closePolicy(org.yuemi.libs.api.gui.ClosePolicy.CLOSE)
                .onClose(p -> {})
                .build()
                .open(player);
    }

    private GuiItem createBackgroundItem() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);
        return GuiItem.builder()
                .item(filler)
                .build();
    }

    private void buildSlotLayer(Player player, org.yuemi.libs.api.gui.GuiLayerBuilder layer) {
        AccessorySlot[] slots = AccessorySlot.values();
        for (int i = 0; i < slots.length; i++) {
            AccessorySlot slot = slots[i];
            int max = api.getMaxSlots(slot);

            for (int idx = 0; idx < max; idx++) {
                int slotPos = calculateSlotPosition(i, idx, max);
                Optional<ItemStack> equipped = api.getEquipped(player, slot, idx);
                GuiItem item = createSlotItem(player, slot, idx, equipped.orElse(null));
                layer.setItem(slotPos, item);
            }
        }
    }

    private void buildActionLayer(Player player, org.yuemi.libs.api.gui.GuiLayerBuilder layer) {
        int unequipAllSlot = 49;
        GuiItem unequipAll = GuiItem.builder()
                .item(new ItemStack(Material.RED_DYE))
                .onClick((p, ctx) -> unequipAll(p))
                .build();
        layer.setItem(unequipAllSlot, unequipAll);
    }

    private GuiItem createSlotItem(Player player, AccessorySlot slot, int slotIndex, ItemStack content) {
        if (content != null) {
            return GuiItem.builder()
                    .item(content)
                    .onClick((p, ctx) -> handleUnequip(p, slot, slotIndex))
                    .build();
        } else {
            ItemStack placeholder = new ItemStack(getSlotIcon(slot));
            org.bukkit.inventory.meta.ItemMeta meta = placeholder.getItemMeta();
            meta.setDisplayName("<gray><italic>Empty " + slot.name().toLowerCase() + " slot</italic></gray>");
            placeholder.setItemMeta(meta);
            return GuiItem.builder()
                    .item(placeholder)
                    .onClick((p, ctx) -> handleEquipFromInventory(p, slot, slotIndex))
                    .build();
        }
    }

    private Material getSlotIcon(AccessorySlot slot) {
        return switch (slot) {
            case BELT -> Material.LIGHT_GRAY_STAINED_GLASS_PANE;
            case GLOVES -> Material.WHITE_STAINED_GLASS_PANE;
            case RING -> Material.YELLOW_STAINED_GLASS_PANE;
            case NECKLACE -> Material.ORANGE_STAINED_GLASS_PANE;
        };
    }

    private int calculateSlotPosition(int slotIndex, int subIndex, int max) {
        int baseRow = (slotIndex * 2) + 1;
        int baseCol = (subIndex * 2) + 3;
        return baseRow * 9 + baseCol;
    }

    private void handleEquipFromInventory(Player player, AccessorySlot slot, int slotIndex) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            api.sendMessage(player, "<red>Hold the item you want to equip in your main hand!");
            return;
        }

        if (!api.canEquipToSlot(item, slot)) {
            api.sendMessage(player, "<red>This item cannot be equipped to " + slot.name() + "!");
            return;
        }

        if (api.equipAccessory(player, slot, slotIndex, item)) {
            item.setAmount(0);
            api.sendMessage(player, "<green>Equipped to " + slot.name() + "!");
            GuiProvider.getApi().createBuilder().build();
        } else {
            api.sendMessage(player, "<red>Failed to equip item!");
        }
    }

    private void handleUnequip(Player player, AccessorySlot slot, int slotIndex) {
        if (api.unequipAccessory(player, slot, slotIndex)) {
            api.sendMessage(player, "<green>Unequipped!");
        }
    }

    private void unequipAll(Player player) {
        int count = 0;
        for (AccessorySlot slot : AccessorySlot.values()) {
            for (int i = 0; i < api.getMaxSlots(slot); i++) {
                if (api.unequipAccessory(player, slot, i)) {
                    count++;
                }
            }
        }
        api.sendMessage(player, "<green>Unequipped " + count + " items!");
    }
}
