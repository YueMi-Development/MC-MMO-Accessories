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

        guiApi.createBuilder()
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
            return GuiItem.builder()
                    .item(p -> annotateEquipped(content, def))
                    .onClick((p, ctx) -> {
                        // Click to unequip
                        api.unequipAccessory(p, slot, slotIndex);
                        if (p.getInventory().firstEmpty() != -1) {
                            p.getInventory().addItem(content);
                        } else {
                            p.getWorld().dropItemNaturally(p.getLocation(), content);
                        }
                        api.sendMessage(p, "<green>Unequipped!");
                    })
                    .build();
        } else {
            return GuiItem.builder()
                    .item(p -> createEmptySlotItemStack(def, slotGui))
                    .onClick((p, ctx) -> {
                        // Empty slot click does nothing - drag items to equip
                    })
                    .build();
        }
    }

    private ItemStack annotateEquipped(ItemStack content, SlotDefinition def) {
        ItemStack display = content.clone();
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.lore();
            if (lore == null) lore = new ArrayList<>();
            else lore = new ArrayList<>(lore);
            // Remove any existing accessory-type markers we previously added
            lore.removeIf(c -> {
                String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                        .plainText().serialize(c);
                return plain.startsWith("Type: ") || plain.startsWith("Slot: ");
            });
            lore.add(Component.empty());
            lore.add(Component.text("Slot: ").color(NamedTextColor.DARK_GRAY)
                    .append(Component.text(def.displayName()).color(NamedTextColor.GRAY)));
            lore.add(Component.text("Type: ").color(NamedTextColor.DARK_GRAY)
                    .append(Component.text(def.requiredType()).color(NamedTextColor.GRAY)));
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.values());
            display.setItemMeta(meta);
        }
        return display;
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
