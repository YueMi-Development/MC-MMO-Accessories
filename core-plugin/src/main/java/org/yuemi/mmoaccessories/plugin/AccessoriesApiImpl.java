package org.yuemi.mmoaccessories.plugin;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.yuemi.mmoaccessories.api.AccessoriesApi;
import org.yuemi.mmoaccessories.api.AccessoriesApiProvider;
import org.yuemi.mmoaccessories.api.AccessorySlot;
import org.yuemi.mmoaccessories.api.SlotDefinition;
import org.yuemi.mmoaccessories.api.storage.AccessoryStorage;
import org.yuemi.mmoaccessories.plugin.config.GuiConfig;
import org.yuemi.mmoaccessories.plugin.config.SlotConfig;
import org.yuemi.mmoaccessories.plugin.gui.AccessoriesGui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.yuemi.mmoitems.api.MmoItemsApi;

public class AccessoriesApiImpl implements AccessoriesApi {

    private final AccessoriesPlugin plugin;
    private final SlotConfig slotConfig;
    private GuiConfig guiConfig;
    private AccessoryStorage storage;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final NamespacedKey itemIdKey;
    private AccessoriesGui gui;

    public AccessoriesApiImpl(AccessoriesPlugin plugin, SlotConfig slotConfig) {
        this.plugin = plugin;
        this.slotConfig = slotConfig;
        this.itemIdKey = new NamespacedKey(plugin, "id");
    }

    public void init() {
        this.guiConfig = GuiConfig.loadFromConfig(plugin.getConfig());
        this.storage = plugin.getStorage();
        this.gui = new AccessoriesGui(this, slotConfig, guiConfig);
    }

    public void reload() {
        this.guiConfig = GuiConfig.loadFromConfig(plugin.getConfig());
        this.storage = plugin.getStorage();
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

        List<String> accepted = slotConfig.getAcceptedTypes(slot);
        if (accepted.isEmpty()) {
            return false;
        }

        List<String> itemTypes = getMmoItemTypes(item);
        // If MmoItems is not installed or item has no types, allow fallback
        // (e.g., when item is a vanilla item, fall back to material name)
        if (itemTypes.isEmpty()) {
            // Fallback: check if item material name matches any accepted type
            String materialName = item.getType().name();
            return accepted.stream().anyMatch(t -> t.equalsIgnoreCase(materialName));
        }

        return itemTypes.stream().anyMatch(accepted::contains);
    }

    @Override
    public boolean isAccessoryType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        List<String> types = getMmoItemTypes(item);
        return types.contains("ACCESSORY");
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

    /**
     * Returns the MmoItems API via its registered Provider, or null when the soft-depend
     * is missing or not yet initialised.
     */
    private MmoItemsApi mmoItemsApi() {
        try {
            // MmoItems registers its API directly under MmoItemsApi.class
            // (not via MmoItemsApiProvider), so look it up by the API class.
            return Bukkit.getServicesManager().load(MmoItemsApi.class);
        } catch (Throwable t) {
            return null;
        }
    }

    private List<String> getMmoItemTypes(ItemStack item) {
        MmoItemsApi api = mmoItemsApi();
        if (api == null) return List.of();
        try {
            List<String> types = api.getItemTypes(item);
            return types != null ? types : List.of();
        } catch (Exception ignored) {
            return List.of();
        }
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
        // First check our own PDC
        var pdc = item.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(itemIdKey, PersistentDataType.STRING);
        if (id != null && !id.isEmpty()) {
            return id;
        }
        // Fall back to MmoItems PDC via typed Provider lookup
        MmoItemsApi api = mmoItemsApi();
        if (api != null) {
            try {
                Optional<String> mmoId = api.getCustomItemId(item);
                if (mmoId.isPresent()) {
                    return mmoId.get();
                }
            } catch (Exception ignored) {
                // fall through
            }
        }
        // Final fallback: use material name
        return item.getType().name().toLowerCase();
    }

    private Optional<ItemStack> createItemFromId(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return Optional.empty();
        }

        MmoItemsApi api = mmoItemsApi();
        if (api != null) {
            try {
                ItemStack item = api.createCustomItem(itemId);
                if (item != null) {
                    return Optional.of(item);
                }
            } catch (Exception ignored) {
                // fall through to material fallback
            }
        }

        try {
            org.bukkit.Material material = org.bukkit.Material.valueOf(itemId.toUpperCase());
            return Optional.of(new ItemStack(material));
        } catch (Exception ignored) {}

        return Optional.empty();
    }

    private List<String> getAccessoriesList(Player player) {
        if (storage == null) return new ArrayList<>();
        return storage.loadAccessories(player.getUniqueId());
    }

    private void saveAccessories(Player player, List<String> accessories) {
        if (storage == null) return;
        storage.saveAccessories(player.getUniqueId(), accessories);
    }
}
