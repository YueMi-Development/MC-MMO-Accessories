package org.yuemi.mmoaccessories.plugin.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.yuemi.mmomechanics.api.MmoMechanicsApi;
import org.yuemi.mmoaccessories.api.AccessoriesApi;
import org.yuemi.mmoitems.api.MmoItemsApi;
import org.yuemi.mmoitems.api.MmoItemsApiProvider;
import org.yuemi.mmoitems.api.item.ItemSkillConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Listens to player actions and fires mechanics from equipped accessories.
 *
 * <p>Accessories in GUI slots are treated as if their skill bindings were present
 * on an item the player is actively using: every Bukkit action that has a matching
 * trigger fires the MmoMechanics skill declared on the accessory's MmoItems config.
 *
 * <p>Recursion guard prevents a skill's own damage from re-triggering itself.
 */
public class AccessoryInteractionListener implements Listener {

    private final AccessoriesApi api;
    private final Logger logger;
    private final MmoMechanicsApi mechanicsApi;
    private final Supplier<MmoItemsApi> itemsApiSupplier;
    private final Set<UUID> activeCasters = new HashSet<>();
    private final AtomicReference<MmoItemsApi> lastItemsApi = new AtomicReference<>();

    public AccessoryInteractionListener(AccessoriesApi api, Logger logger, MmoMechanicsApi mechanicsApi) {
        this(api, logger, mechanicsApi, AccessoryInteractionListener::resolveMmoItemsApi);
    }

    /**
     * Constructor used by tests to inject a stub {@link MmoItemsApi}.
     */
    AccessoryInteractionListener(
            AccessoriesApi api,
            Logger logger,
            MmoMechanicsApi mechanicsApi,
            Supplier<MmoItemsApi> itemsApiSupplier
    ) {
        this.api = api;
        this.logger = logger;
        this.mechanicsApi = mechanicsApi;
        this.itemsApiSupplier = itemsApiSupplier;
    }

    /** Returns the items API most recently used by the listener (test introspection). */
    public MmoItemsApi getLastItemsApi() {
        return lastItemsApi.get();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        triggerForEvent(event.getPlayer(), "onRightClick");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            triggerForEvent(player, "onAttack");
        }
    }

    private void triggerForEvent(Player player, String trigger) {
        if (mechanicsApi == null) {
            return;
        }
        if (!api.isFeatureEnabled(player)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        if (activeCasters.contains(uuid)) {
            return;
        }
        if (!api.isAvailable()) {
            return;
        }

        List<ItemStack> accessories;
        try {
            accessories = api.getAllEquipped(player);
        } catch (Exception e) {
            logger.warning("Failed to read equipped accessories for " + player.getName() + ": " + e.getMessage());
            return;
        }
        if (accessories == null || accessories.isEmpty()) {
            return;
        }

        MmoItemsApi itemsApi = itemsApiSupplier.get();
        lastItemsApi.set(itemsApi);
        if (itemsApi == null) {
            return;
        }

        activeCasters.add(uuid);
        try {
            for (ItemStack item : accessories) {
                if (item == null || !item.hasItemMeta()) continue;
                if (!itemsApi.isCustomItem(item)) continue;

                List<ItemSkillConfig> bindings;
                try {
                    bindings = itemsApi.getItemSkills(item);
                } catch (Exception e) {
                    continue;
                }
                if (bindings == null || bindings.isEmpty()) continue;

                for (ItemSkillConfig binding : bindings) {
                    if (binding == null || binding.skill() == null || binding.skill().isEmpty()) continue;
                    String bindingTrigger = binding.trigger();
                    if (bindingTrigger == null) {
                        continue;
                    }
                    if (!bindingTrigger.equalsIgnoreCase(trigger)) continue;

                    String skillId = binding.skill();
                    try {
                        mechanicsApi.castSkill(uuid, skillId);
                    } catch (Exception e) {
                        logger.warning("Failed to cast skill '" + skillId + "' for " + player.getName() + ": " + e.getMessage());
                    }
                }
            }
        } finally {
            activeCasters.remove(uuid);
        }
    }

    public static AccessoryInteractionListener create(AccessoriesApi api, Logger logger) {
        MmoMechanicsApi mechanicsApi = Bukkit.getServicesManager().load(MmoMechanicsApi.class);
        return new AccessoryInteractionListener(api, logger, mechanicsApi);
    }

    private static MmoItemsApi resolveMmoItemsApi() {
        try {
            MmoItemsApiProvider provider = Bukkit.getServicesManager().load(MmoItemsApiProvider.class);
            return provider != null ? provider.getApi() : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
