package org.yuemi.mmoaccessories.plugin.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.yuemi.libs.api.gui.GuiApi;
import org.yuemi.libs.api.gui.GuiItem;
import org.yuemi.libs.api.gui.GuiProvider;
import org.yuemi.mmoaccessories.api.AccessoriesApi;
import org.yuemi.mmoaccessories.api.AccessorySlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * GUI for managing player accessories.
 * Uses YueMiLibs GuiApi for GUI creation and ItemsApi for vanilla item lookups.
 * Item styling uses Component (Adventure) directly for proper text rendering.
 */
public final class AccessoriesGui {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final String GUI_TITLE = "Accessories";

    private static final String MINECRAFT = "minecraft:";
    private static final String KEY_BACKGROUND = MINECRAFT + "black_stained_glass_pane";
    private static final String KEY_BORDER = MINECRAFT + "gray_stained_glass_pane";
    private static final String KEY_NAME_TAG = MINECRAFT + "name_tag";
    private static final String KEY_BELT_EMPTY = MINECRAFT + "light_gray_stained_glass_pane";
    private static final String KEY_GLOVES_EMPTY = MINECRAFT + "white_stained_glass_pane";
    private static final String KEY_RING_EMPTY = MINECRAFT + "yellow_stained_glass_pane";
    private static final String KEY_NECKLACE_EMPTY = MINECRAFT + "orange_stained_glass_pane";
    private static final String KEY_UNEQUIP_ALL = MINECRAFT + "barrier";
    private static final String KEY_INFO = MINECRAFT + "writable_book";
    private static final String KEY_CLOSE = MINECRAFT + "arrow";

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
                .createLayer("labels", 3, layer -> buildLabelLayer(layer))
                .closePolicy(org.yuemi.libs.api.gui.ClosePolicy.CLOSE)
                .onClose(p -> {})
                .build()
                .open(player);
    }

    private ItemStack fetchVanilla(String key) {
        var itemsApi = org.yuemi.libs.api.items.ItemsApiProvider.getApi();
        if (itemsApi != null) {
            ItemStack item = itemsApi.getItem(key, 1);
            if (item != null && item.getType() != Material.AIR) {
                return item;
            }
        }
        return new ItemStack(Material.STONE);
    }

    private GuiItem createBackgroundItem() {
        return GuiItem.builder()
                .item(p -> {
                    ItemStack item = fetchVanilla(KEY_BACKGROUND);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.displayName(Component.text(" "));
                        item.setItemMeta(meta);
                    }
                    return item;
                })
                .build();
    }

    private GuiItem createBorderItem() {
        return GuiItem.builder()
                .item(p -> {
                    ItemStack item = fetchVanilla(KEY_BORDER);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.displayName(Component.text(" "));
                        item.setItemMeta(meta);
                    }
                    return item;
                })
                .build();
    }

    private void buildSlotLayer(Player player, org.yuemi.libs.api.gui.GuiLayerBuilder layer) {
        AccessorySlot[] slots = AccessorySlot.values();
        for (int i = 0; i < slots.length; i++) {
            AccessorySlot slot = slots[i];
            int max = api.getMaxSlots(slot);

            int labelPos = getSlotLabelPosition(i);
            layer.setItem(labelPos, createSlotLabel(slot, max));

            for (int idx = 0; idx < max; idx++) {
                int slotPos = calculateSlotPosition(i, idx, max);
                Optional<ItemStack> equipped = api.getEquipped(player, slot, idx);
                GuiItem item = createSlotItem(player, slot, idx, equipped.orElse(null));
                layer.setItem(slotPos, item);
            }
        }
    }

    private int getSlotLabelPosition(int slotIndex) {
        return (slotIndex * 2 + 1) * 9 + 1;
    }

    private GuiItem createSlotLabel(AccessorySlot slot, int max) {
        return GuiItem.builder()
                .item(p -> createLabelItemStack(slot, max))
                .build();
    }

    private ItemStack createLabelItemStack(AccessorySlot slot, int max) {
        ItemStack label = fetchVanilla(KEY_NAME_TAG);
        ItemMeta meta = label.getItemMeta();
        if (meta != null) {
            meta.displayName(getSlotLabelName(slot));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Max slots: ").color(NamedTextColor.GRAY)
                    .append(Component.text(max).color(NamedTextColor.YELLOW)));
            lore.add(Component.text("Type required: ").color(NamedTextColor.GRAY)
                    .append(Component.text(api.getSlotRequiredType(slot)).color(NamedTextColor.GREEN)));
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.values());
            label.setItemMeta(meta);
        }
        return label;
    }

    private Component getSlotLabelName(AccessorySlot slot) {
        return switch (slot) {
            case BELT -> Component.text("⚜ Belt Slot")
                    .color(NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true);
            case GLOVES -> Component.text("✦ Gloves Slots")
                    .color(NamedTextColor.AQUA)
                    .decoration(TextDecoration.BOLD, true);
            case RING -> Component.text("❖ Ring Slots")
                    .color(NamedTextColor.YELLOW)
                    .decoration(TextDecoration.BOLD, true);
            case NECKLACE -> Component.text("♦ Necklace Slot")
                    .color(NamedTextColor.LIGHT_PURPLE)
                    .decoration(TextDecoration.BOLD, true);
        };
    }

    private String getEmptyKey(AccessorySlot slot) {
        return switch (slot) {
            case BELT -> KEY_BELT_EMPTY;
            case GLOVES -> KEY_GLOVES_EMPTY;
            case RING -> KEY_RING_EMPTY;
            case NECKLACE -> KEY_NECKLACE_EMPTY;
        };
    }

    private void buildActionLayer(Player player, org.yuemi.libs.api.gui.GuiLayerBuilder layer) {
        layer.setItem(49, GuiItem.builder()
                .item(p -> {
                    ItemStack item = fetchVanilla(KEY_UNEQUIP_ALL);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.displayName(Component.text("✖ Unequip All")
                                .color(NamedTextColor.RED)
                                .decoration(TextDecoration.BOLD, true));
                        List<Component> lore = new ArrayList<>();
                        lore.add(Component.text("Removes all equipped accessories.").color(NamedTextColor.DARK_GRAY));
                        lore.add(Component.empty());
                        lore.add(Component.text("Click to confirm").color(NamedTextColor.RED));
                        meta.lore(lore);
                        meta.addItemFlags(ItemFlag.values());
                        item.setItemMeta(meta);
                    }
                    return item;
                })
                .onClick((p, ctx) -> unequipAll(p))
                .build());

        layer.setItem(45, GuiItem.builder()
                .item(p -> createInfoItemStack(p))
                .build());

        layer.setItem(53, GuiItem.builder()
                .item(p -> {
                    ItemStack item = fetchVanilla(KEY_CLOSE);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.displayName(Component.text("✕ Close")
                                .color(NamedTextColor.RED)
                                .decoration(TextDecoration.BOLD, true));
                        List<Component> lore = new ArrayList<>();
                        lore.add(Component.text("Close this menu").color(NamedTextColor.GRAY));
                        meta.lore(lore);
                        meta.addItemFlags(ItemFlag.values());
                        item.setItemMeta(meta);
                    }
                    return item;
                })
                .onClick((p, ctx) -> p.closeInventory())
                .build());
    }

    private ItemStack createInfoItemStack(Player player) {
        ItemStack infoItem = fetchVanilla(KEY_INFO);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(Component.text("ℹ How to Use")
                    .color(NamedTextColor.AQUA)
                    .decoration(TextDecoration.BOLD, true));
            List<Component> infoLore = new ArrayList<>();
            infoLore.add(Component.text("Hold an accessory in your").color(NamedTextColor.GRAY));
            infoLore.add(Component.text("main hand and click an").color(NamedTextColor.GRAY));
            infoLore.add(Component.text("empty slot to equip it.").color(NamedTextColor.GRAY));
            infoLore.add(Component.empty());
            infoLore.add(Component.text("Click an equipped item").color(NamedTextColor.GRAY));
            infoLore.add(Component.text("to unequip it.").color(NamedTextColor.GRAY));
            infoLore.add(Component.empty());
            infoLore.add(Component.text("─────────────").color(NamedTextColor.DARK_GRAY));
            infoLore.add(Component.text("Total slots: ").color(NamedTextColor.GRAY)
                    .append(Component.text(getTotalSlotCount()).color(NamedTextColor.GOLD)));
            infoLore.add(Component.text("Equipped: ").color(NamedTextColor.GRAY)
                    .append(Component.text(api.getAllEquipped(player).size()).color(NamedTextColor.GREEN)));
            infoMeta.lore(infoLore);
            infoMeta.addItemFlags(ItemFlag.values());
            infoItem.setItemMeta(infoMeta);
        }
        return infoItem;
    }

    private void buildLabelLayer(org.yuemi.libs.api.gui.GuiLayerBuilder layer) {
        for (int i = 0; i < 9; i++) {
            layer.setItem(i, createBorderItem());
            layer.setItem(45 + i, createBorderItem());
        }
        for (int row = 1; row < 5; row++) {
            layer.setItem(row * 9, createBorderItem());
            layer.setItem(row * 9 + 8, createBorderItem());
        }
    }

    private int getTotalSlotCount() {
        int total = 0;
        for (AccessorySlot slot : AccessorySlot.values()) {
            total += api.getMaxSlots(slot);
        }
        return total;
    }

    private GuiItem createSlotItem(Player player, AccessorySlot slot, int slotIndex, ItemStack content) {
        if (content != null) {
            return GuiItem.builder()
                    .item(content)
                    .onClick((p, ctx) -> handleUnequip(p, slot, slotIndex))
                    .build();
        } else {
            return GuiItem.builder()
                    .item(p -> createEmptySlotItemStack(slot))
                    .onClick((p, ctx) -> handleEquipFromInventory(p, slot, slotIndex))
                    .build();
        }
    }

    private ItemStack createEmptySlotItemStack(AccessorySlot slot) {
        ItemStack placeholder = fetchVanilla(getEmptyKey(slot));
        ItemMeta meta = placeholder.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Empty " + slot.name().toLowerCase() + " slot")
                    .color(NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, true));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Hold an accessory in your").color(NamedTextColor.GRAY));
            lore.add(Component.text("main hand and click to equip.").color(NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("▶ Click to equip").color(NamedTextColor.GREEN));
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.values());
            placeholder.setItemMeta(meta);
        }
        return placeholder;
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
