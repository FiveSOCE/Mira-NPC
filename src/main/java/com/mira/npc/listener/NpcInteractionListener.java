package com.mira.npc.listener;

import com.mira.npc.MiraNPCPlugin;
import com.mira.npc.model.NpcDefinition;
import com.mira.npc.service.NpcService;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerLeashEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class NpcInteractionListener implements Listener {
    private final MiraNPCPlugin plugin;
    private final NpcService service;

    public NpcInteractionListener(MiraNPCPlugin plugin, NpcService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onRightClick(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!service.isNpc(event.getRightClicked())) return;
        event.setCancelled(true);
        run(event.getPlayer(), (Villager) event.getRightClicked());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageEvent event) {
        if (!service.isNpc(event.getEntity())) return;
        event.setCancelled(true);
        if (event instanceof org.bukkit.event.entity.EntityDamageByEntityEvent byEntity && byEntity.getDamager() instanceof Player player) {
            run(player, (Villager) event.getEntity());
        }
    }

    @EventHandler(ignoreCancelled = false)
    public void onCombust(EntityCombustEvent event) {
        if (service.isNpc(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false)
    public void onTarget(EntityTargetEvent event) {
        if (service.isNpc(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false)
    public void onTeleport(EntityTeleportEvent event) {
        if (service.isNpc(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false)
    public void onTransform(EntityTransformEvent event) {
        if (service.isNpc(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false)
    public void onLeash(PlayerLeashEntityEvent event) {
        if (service.isNpc(event.getEntity())) event.setCancelled(true);
    }

    private void run(Player player, Villager villager) {
        NpcDefinition definition = service.definition(villager).orElse(null);
        if (definition == null) return;
        if (!service.execute(player, definition)) {
            plugin.msg(player, plugin.message("command-failed"));
        }
    }
}
