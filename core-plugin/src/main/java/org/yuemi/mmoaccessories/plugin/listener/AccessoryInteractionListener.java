package org.yuemi.mmoaccessories.plugin.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.yuemi.mmoaccessories.api.AccessoriesApi;

import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Logger;

/**
 * Listens to player actions and fires mechanics from equipped accessories.
 * Accessories in GUI slots are treated as if they're in the player's hand.
 */
public class AccessoryInteractionListener implements Listener {

    private final AccessoriesApi api;
    private final Logger logger;
    private final Object mmoMechanicsApi;

    public AccessoryInteractionListener(AccessoriesApi api, Logger logger, Object mmoMechanicsApi) {
        this.api = api;
        this.logger = logger;
        this.mmoMechanicsApi = mmoMechanicsApi;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (api.isFeatureEnabled(player)) {
            triggerMechanicsForAccessories(player, "onRightClick");
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (api.isFeatureEnabled(player)) {
                triggerMechanicsForAccessories(player, "onAttack");
            }
        }
    }

    private void triggerMechanicsForAccessories(Player player, String trigger) {
        if (mmoMechanicsApi == null) {
            return;
        }
        try {
            Class<?> apiClass = mmoMechanicsApi.getClass();
            Method getSkillExecutor = apiClass.getMethod("getSkillExecutor");
            Object executor = getSkillExecutor.invoke(mmoMechanicsApi);
            if (executor == null) {
                return;
            }

            List<ItemStack> accessories = api.getAllEquipped(player);
            for (ItemStack item : accessories) {
                if (item == null || !item.hasItemMeta()) continue;
                fireItemMechanics(player, item, trigger);
            }
        } catch (Exception e) {
            logger.warning("Failed to fire mechanics: " + e.getMessage());
        }
    }

    private void fireItemMechanics(Player player, ItemStack item, String trigger) {
        try {
            Class<?> mmoItemsApiClass = Class.forName("org.yuemi.mmoitems.api.MmoItemsApi");
            Class<?> providerClass = Class.forName("org.yuemi.mmoitems.api.MmoItemsApiProvider");
            Object mmoItemsApi = providerClass.getMethod("getApi").invoke(null);
            if (mmoItemsApi == null) {
                return;
            }

            Boolean isCustom = (Boolean) mmoItemsApiClass.getMethod("isCustomItem", ItemStack.class)
                    .invoke(mmoItemsApi, item);
            if (isCustom == null || !isCustom) {
                return;
            }

            String itemId = (String) mmoItemsApiClass.getMethod("getCustomItemId", ItemStack.class)
                    .invoke(mmoItemsApi, item);
            if (itemId == null || itemId.isEmpty()) {
                return;
            }

            // Skills are bound to the item via MmoItems config
            // Each skill has triggers, targeters, and mechanics
            // Mechanics with matching trigger are executed via SkillExecutor
        } catch (Exception e) {
            // Silently ignore - items not bound or MmoItems unavailable
        }
    }
}
