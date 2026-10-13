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
import org.yuemi.libs.api.gui.SlotBehavior;
import org.yuemi.mmoaccessories.api.AccessorySlot;
import org.yuemi.mmoaccessories.api.AccessorySlotGui;
import org.yuemi.mmoaccessories.api.AccessorySlotId;
import org.yuemi.mmoaccessories.api.SlotDefinition;
import org.yuemi.mmoaccessories.plugin.AccessoriesApiImpl;
import org.yuemi.mmoaccessories.plugin.config.GuiConfig;
import org.yuemi.mmoaccessories.plugin.config.SlotConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * GUI for managing player accessories.
 * Uses YueMiLibs GuiApi for GUI creation and ItemsApi for vanilla item lookups.
 * Item styling uses Component (Adventure) directly.
 * Slot positions are configured via config.yml.
 */
public final class AccessoriesGui {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final AccessoriesApiImpl api;
    private final SlotConfig slotConfig;
    private final GuiConfig guiConfig;

    // Maps inventory slot → SlotMapping
    private final Map<Integer, SlotMapping> slotMappings = new HashMap<>();

    public AccessoriesGui(AccessoriesApiImpl api, SlotConfig slotConfig, GuiConfig guiConfig) {
        this.api = api;
        this.slotConfig = slotConfig;
        this.guiConfig = guiConfig;
        buildSlotMappings();
    }

    private void buildSlotMappings() {
        slotMappings.clear();
        for (Map.Entry<AccessorySlotId, SlotDefinition> entry : slotConfig.getAllSlotDefinitions().entrySet()) {
            AccessorySlotId slotId = entry.getKey();
            AccessorySlotGui gui = entry.getValue().gui();
            if (gui == null) continue;
            List<Integer> positions = gui.slots();
            for (int i = 0; i < positions.size(); i++) {
                AccessorySlot enumSlot = slotId.isBuiltin() ? slotId.builtin() : AccessorySlot.values()[0];
                slotMappings.put(positions.get(i), new SlotMapping(enumSlot, slotId, i));
            }
        }
    }

    public void open(Player player) {
        GuiApi guiApi = GuiProvider.getApi();
        if (guiApi == null) {
            api.sendMessage(player, "<red>YueMiLibs GUI is not available!");
            return;
        }

        // Rebuild mappings in case config was reloaded
        buildSlotMappings();

        var gui = guiApi.createBuilder()
                .title(guiConfig.getTitle())
                .rows(guiConfig.getRows())
                .createLayer("background", 0, layer -> {
                    if (guiConfig.isBackgroundEnabled()) {
                        layer.fill(createBackgroundItem());
                    }
                })
                .createLayer("slots", 1, layer -> buildSlotLayer(player, layer))
                .createLayer("actions", 2, layer -> buildActionLayer(player, layer))
                .closePolicy(org.yuemi.libs.api.gui.ClosePolicy.CLOSE)
                .onClose(p -> {})
                .build();

        // Register all accessory slot positions as "input slots" so the GUI
        // listener knows items can be placed into them (vs swapping with existing items).
        registerInputSlots(slotMappings.keySet());

        gui.open(player);
    }

    /**
     * Register slot positions as input slots via reflection (avoids hard dependency
     * on internal YueMiLibs implementation class).
     */
    private static void registerInputSlots(java.util.Set<Integer> positions) {
        try {
            Class<?> apiClass = Class.forName("org.yuemi.libs.plugin.gui.GuiApiImpl");
            java.lang.reflect.Method method = apiClass.getMethod("registerInputSlots", java.util.Collection.class);
            method.invoke(null, positions);
        } catch (Exception e) {
            // Silently fail if YueMiLibs version doesn't support this
        }
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
                    ItemStack item = fetchVanilla(guiConfig.getBackgroundIcon());
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
        for (Map.Entry<Integer, SlotMapping> entry : slotMappings.entrySet()) {
            int invSlot = entry.getKey();
            SlotMapping mapping = entry.getValue();
            Optional<ItemStack> equipped = api.getEquipped(player, mapping.slot, mapping.index);
            GuiItem item = createSlotItem(player, mapping.slot, mapping.index, equipped.orElse(null));
            layer.setItem(invSlot, item);
        }
    }

    private void buildActionLayer(Player player, org.yuemi.libs.api.gui.GuiLayerBuilder layer) {
        // Close button
        GuiConfig.ActionButton close = guiConfig.getClose();
        layer.setItem(close.slot(), GuiItem.builder()
                .item(p -> createActionItem(close))
                .onClick((p, ctx) -> p.closeInventory())
                .build());
    }

    private ItemStack createActionItem(GuiConfig.ActionButton btn) {
        ItemStack item = fetchVanilla(btn.icon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize(btn.name()));
            List<Component> lore = new ArrayList<>();
            for (String line : btn.lore()) {
                lore.add(MM.deserialize(line));
            }
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    private GuiItem createSlotItem(Player player, AccessorySlot slot, int slotIndex, ItemStack content) {
        SlotDefinition def = slotConfig.getSlotDefinition(slot);
        AccessorySlotGui slotGui = def.gui();

        if (content != null) {
            // Filled slot - allow pickup only (output)
            return GuiItem.builder()
                    .item(p -> annotateEquipped(content, def))
                    .behavior(SlotBehavior.output())
                    .onClick((p, ctx) -> handleSlotClick(p, ctx, slot, slotIndex, content, true))
                    .build();
        } else {
            // Empty slot - allow place from inventory (input)
            return GuiItem.builder()
                    .item(p -> createEmptySlotItemStack(def, slotGui))
                    .behavior(SlotBehavior.input())
                    .onClick((p, ctx) -> handleSlotClick(p, ctx, slot, slotIndex, null, false))
                    .build();
        }
    }

    /**
     * Handle slot interaction based on the action type.
     * - For empty slots: PLACE / DRAG_PLACE / SHIFT_CLICK → equip from cursor
     * - For filled slots: PICKUP / SWAP → unequip to inventory
     */
    private void handleSlotClick(Player player, org.yuemi.libs.api.gui.GuiClickContext ctx,
                                 AccessorySlot slot, int slotIndex, ItemStack currentItem, boolean isFilled) {
        boolean changed = false;
        if (isFilled) {
            // Pickup flow: item moves to cursor naturally (PICKUP action)
            // For SWAP/MOVE actions, we need to handle manually
            if (currentItem == null) return;
            if (api.unequipAccessory(player, slot, slotIndex)) {
                // The item was already moved to cursor by the GUI system
                api.sendMessage(player, "<green>Unequipped!");
                changed = true;
            }
        } else {
            // Empty slot - try to equip from cursor
            ItemStack cursor = ctx.getInteractedItem();
            if (cursor == null || cursor.getType() == Material.AIR) {
                api.sendMessage(player, "<red>Hold an item in your cursor or drag an item to equip!");
                return;
            }
            if (!api.canEquipToSlot(cursor, slot)) {
                api.sendMessage(player, "<red>This item cannot be equipped to " + slot.name() + "!");
                return;
            }
            if (api.equipAccessory(player, slot, slotIndex, cursor.clone())) {
                // Reduce cursor by 1 (we cloned the item to equip it)
                cursor.setAmount(cursor.getAmount() - 1);
                if (cursor.getAmount() <= 0) {
                    player.setItemOnCursor(null);
                }
                api.sendMessage(player, "<green>Equipped to " + slot.name() + "!");
                changed = true;
            } else {
                api.sendMessage(player, "<red>Failed to equip item!");
            }
        }

        // Refresh the GUI to show the new state of slots
        if (changed) {
            refreshGui(player);
        }
    }

    /**
     * Refresh the GUI for the player by re-rendering all slots.
     * This is needed because the GUI caches the slot items at open time,
     * so after equip/unequip we need to re-populate.
     */
    private void refreshGui(Player player) {
        // Use Bukkit task to defer the refresh to next tick,
        // avoiding modifying inventory during click event processing
        org.bukkit.Bukkit.getScheduler().runTask(
                org.yuemi.mmoaccessories.plugin.AccessoriesPlugin.getInstance(),
                () -> {
                    // Force re-open the GUI with fresh state
                    open(player);
                }
        );
    }

    private ItemStack annotateEquipped(ItemStack content, SlotDefinition def) {
        // Return the item as-is without appending slot/type lore
        return content.clone();
    }

    private ItemStack createEmptySlotItemStack(SlotDefinition def, AccessorySlotGui gui) {
        ItemStack placeholder = fetchVanilla(gui.icon());
        ItemMeta meta = placeholder.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize(gui.labelName()));
            List<Component> lore = new ArrayList<>();
            for (String line : gui.labelLore()) {
                lore.add(MM.deserialize(line));
            }
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.values());
            placeholder.setItemMeta(meta);
        }
        return placeholder;
    }

    public Map<Integer, SlotMapping> getSlotMappings() {
        return slotMappings;
    }

    public GuiConfig getGuiConfig() {
        return guiConfig;
    }

    public SlotConfig getSlotConfig() {
        return slotConfig;
    }

    public record SlotMapping(AccessorySlot slot, AccessorySlotId slotId, int index) {}
}
