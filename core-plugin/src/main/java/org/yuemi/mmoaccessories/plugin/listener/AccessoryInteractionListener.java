package org.yuemi.mmoaccessories.plugin.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.yuemi.libs.api.event.EventApi;
import org.yuemi.libs.api.event.EventApiProvider;
import org.yuemi.mmomechanics.api.MmoMechanicsApi;
import org.yuemi.mmoaccessories.api.AccessoriesApi;
import org.yuemi.mmoitems.api.MmoItemsApi;
import org.yuemi.mmoitems.api.item.ItemSkillConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Listens to player actions and fires mechanics from equipped accessories.
 *
 * <p>Subscribes through YueMiLibs' {@link EventApi} (no direct Paper
 * {@code registerEvents} calls). For each matched event, every equipped
 * accessory whose MmoItems binding has the matching trigger is run via
 * {@link MmoMechanicsApi#castSkill(UUID, String)}. The {@code source_item}
 * context variable is set so conditions like
 * {@code IsEquippedAsAccessoryCondition} can verify the item is still worn.
 *
 * <p>A recursion guard prevents a skill's own damage from re-triggering itself.
 */
public class AccessoryInteractionListener {

    private final AccessoriesApi api;
    private final Logger logger;
    private final MmoMechanicsApi mechanicsApi;
    private final Supplier<MmoItemsApi> itemsApiSupplier;
    private final Set<UUID> activeCasters = new HashSet<>();

    public AccessoryInteractionListener(AccessoriesApi api, Logger logger, MmoMechanicsApi mechanicsApi) {
        this(api, logger, mechanicsApi, AccessoryInteractionListener::resolveMmoItemsApi);
    }

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

    /**
     * Subscribes to PlayerInteractEvent and EntityDamageByEntityEvent through
     * YueMiLibs' EventApi. Call from {@code onEnable}; the subscriptions are
     * tracked by the EventApi subsystem so they clean up on plugin disable.
     */
    public void register() {
        EventApi eventApi = EventApiProvider.getApi();
        if (eventApi == null) {
            logger.warning("YueMiLibs EventApi service not found; AccessoryInteractionListener will not fire.");
            return;
        }

        eventApi.bukkit().subscribe(PlayerInteractEvent.class)
                .priority(EventPriority.MONITOR)
                .ignoreCancelled(true)
                .handler(this::onPlayerInteract);

        eventApi.bukkit().subscribe(EntityDamageByEntityEvent.class)
                .priority(EventPriority.MONITOR)
                .ignoreCancelled(true)
                .handler(this::onEntityDamageByEntity);

        logger.info("AccessoryInteractionListener subscribed via YueMiLibs EventApi.");
    }

    private void onPlayerInteract(PlayerInteractEvent event) {
        triggerForEvent(event.getPlayer(), "onRightClick");
    }

    private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
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

    private static MmoItemsApi resolveMmoItemsApi() {
        try {
            return Bukkit.getServicesManager().load(MmoItemsApi.class);
        } catch (Throwable t) {
            return null;
        }
    }
}
