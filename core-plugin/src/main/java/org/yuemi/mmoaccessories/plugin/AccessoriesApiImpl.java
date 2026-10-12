package org.yuemi.mmoaccessories.plugin;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.yuemi.mmoaccessories.api.AccessoriesApi;
import org.yuemi.mmoaccessories.api.AccessoriesApiProvider;
import org.yuemi.mmoaccessories.api.AccessorySlot;
import org.yuemi.mmoaccessories.api.SlotDefinition;
import org.yuemi.mmoaccessories.plugin.config.GuiConfig;
import org.yuemi.mmoaccessories.plugin.config.SlotConfig;
import org.yuemi.mmoaccessories.plugin.gui.AccessoriesGui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class AccessoriesApiImpl implements AccessoriesApi {

    private final AccessoriesPlugin plugin;
    private final SlotConfig slotConfig;
    private GuiConfig guiConfig;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final NamespacedKey accessoriesKey;
    private final NamespacedKey itemIdKey;
    private AccessoriesGui gui;

    public AccessoriesApiImpl(AccessoriesPlugin plugin, SlotConfig slotConfig) {
        this.plugin = plugin;
        this.slotConfig = slotConfig;
        this.accessoriesKey = new NamespacedKey(plugin, "accessories");
        this.itemIdKey = new NamespacedKey(plugin, "id");
    }

    public void init() {
        this.guiConfig = GuiConfig.loadFromConfig(plugin.getConfig());
        this.gui = new AccessoriesGui(this, slotConfig, guiConfig);
    }

    public void reload() {
        this.guiConfig = GuiConfig.loadFromConfig(plugin.getConfig());
        this.gui = new AccessoriesGui(this, slotConfig, guiConfig);
    }

    @Override
    public boolean equipAccessory(Player player, AccessorySlot slot, int slotIndex, ItemStack item) {
        int maxSlots = slotConfig.getMaxSlots(slot);
        if (slotIndex < 0 || slotIndex >= maxSlots) {
            return false;
        }

        if (!canEquipToSlot(item, slot)) {
            return false;
        }

        List<String> accessories = getAccessoriesList(player);
        int arrayIndex = getSlotOffset(slot) + slotIndex;
        
        String itemId = getItemId(item);
        if (itemId == null) {
            return false;
        }
        
        while (accessories.size() <= arrayIndex) {
            accessories.add("");
        }
        accessories.set(arrayIndex, itemId);
        saveAccessories(player, accessories);
        return true;
    }

    @Override
    public boolean unequipAccessory(Player player, AccessorySlot slot, int slotIndex) {
        int maxSlots = slotConfig.getMaxSlots(slot);
        if (slotIndex < 0 || slotIndex >= maxSlots) {
            return false;
        }

        List<String> accessories = getAccessoriesList(player);
        int arrayIndex = getSlotOffset(slot) + slotIndex;
        
        if (arrayIndex < accessories.size()) {
            accessories.set(arrayIndex, "");
            saveAccessories(player, accessories);
        }
        return true;
    }

    @Override
    public Optional<ItemStack> getEquipped(Player player, AccessorySlot slot, int slotIndex) {
        int maxSlots = slotConfig.getMaxSlots(slot);
        if (slotIndex < 0 || slotIndex >= maxSlots) {
            return Optional.empty();
        }

        List<String> accessories = getAccessoriesList(player);
        int arrayIndex = getSlotOffset(slot) + slotIndex;
        
        if (arrayIndex >= accessories.size()) {
            return Optional.empty();
        }
        
        String itemId = accessories.get(arrayIndex);
        if (itemId == null || itemId.isEmpty()) {
            return Optional.empty();
        }
        
        return createItemFromId(itemId);
    }

    @Override
    public List<ItemStack> getAllEquipped(Player player) {
        List<ItemStack> equipped = new ArrayList<>();
        for (AccessorySlot slot : AccessorySlot.values()) {
            for (int i = 0; i < slotConfig.getMaxSlots(slot); i++) {
                getEquipped(player, slot, i).ifPresent(equipped::add);
            }
        }
        return equipped;
    }

    @Override
    public boolean canEquipToSlot(ItemStack item, AccessorySlot slot) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        String requiredType = slotConfig.getRequiredType(slot);
        return hasMmoItemsType(item, requiredType);
    }

    @Override
    public boolean isAccessoryType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        return hasMmoItemsType(item, "ACCESSORY");
    }

    @Override
    public boolean isSlotFull(Player player, AccessorySlot slot) {
        int maxSlots = slotConfig.getMaxSlots(slot);
        for (int i = 0; i < maxSlots; i++) {
            if (getEquipped(player, slot, i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getSlotRequiredType(AccessorySlot slot) {
        return slotConfig.getRequiredType(slot);
    }

    @Override
    public int getMaxSlots(AccessorySlot slot) {
        return slotConfig.getMaxSlots(slot);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void sendMessage(Player player, String message) {
        player.sendMessage(miniMessage.deserialize(message));
    }

    @Override
    public boolean isFeatureEnabled(Player player) {
        return player.hasPermission("accessories.feature");
    }

    @Override
    public void openGui(Player player) {
        gui.open(player);
    }

    public SlotConfig getSlotConfig() {
        return slotConfig;
    }

    public GuiConfig getGuiConfig() {
        return guiConfig;
    }

    private boolean hasMmoItemsType(ItemStack item, String type) {
        try {
            Class<?> mmoItemsApiClass = Class.forName("org.yuemi.mmoitems.api.MmoItemsApi");
            Class<?> mmoItemsProviderClass = Class.forName("org.yuemi.mmoitems.api.MmoItemsApiProvider");
            Object api = mmoItemsProviderClass.getMethod("getApi").invoke(null);
            if (api != null) {
                Boolean hasType = (Boolean) mmoItemsApiClass.getMethod("hasType", ItemStack.class, String.class)
                        .invoke(api, item, type);
                return hasType != null && hasType;
            }
        } catch (Exception ignored) {}
        
        return false;
    }

    private int getSlotOffset(AccessorySlot slot) {
        return switch (slot) {
            case BELT -> 0;
            case GLOVES -> 1;
            case RING -> 3;
            case NECKLACE -> 5;
        };
    }

    private String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String id = item.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        if (id != null && !id.isEmpty()) {
            return id;
        }
        return item.getType().name().toLowerCase();
    }

    private Optional<ItemStack> createItemFromId(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return Optional.empty();
        }
        
        try {
            Class<?> mmoItemsApiClass = Class.forName("org.yuemi.mmoitems.api.MmoItemsApi");
            Class<?> mmoItemsProviderClass = Class.forName("org.yuemi.mmoitems.api.MmoItemsApiProvider");
            Object api = mmoItemsProviderClass.getMethod("getApi").invoke(null);
            if (api != null) {
                ItemStack item = (ItemStack) mmoItemsApiClass.getMethod("createCustomItem", String.class)
                        .invoke(api, itemId);
                return Optional.ofNullable(item);
            }
        } catch (Exception ignored) {}
        
        try {
            org.bukkit.Material material = org.bukkit.Material.valueOf(itemId.toUpperCase());
            return Optional.of(new ItemStack(material));
        } catch (Exception ignored) {}
        
        return Optional.empty();
    }

    private List<String> getAccessoriesList(Player player) {
        List<String> defaultList = new ArrayList<>();
        
        String stored = player.getPersistentDataContainer().get(accessoriesKey, PersistentDataType.STRING);
        if (stored != null && !stored.isEmpty()) {
            String[] parts = stored.split("\\|");
            for (String part : parts) {
                defaultList.add(part.isEmpty() ? "" : part);
            }
        }
        
        return defaultList;
    }

    private void saveAccessories(Player player, List<String> accessories) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < accessories.size(); i++) {
            if (i > 0) sb.append("|");
            sb.append(accessories.get(i));
        }
        player.getPersistentDataContainer().set(accessoriesKey, PersistentDataType.STRING, sb.toString());
    }
}
