package com.mira.npc.service;

import com.mira.npc.MiraNPCPlugin;
import com.mira.npc.model.NpcDefinition;
import com.mira.npc.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Villager;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

public final class NpcDisplayService {
    private static final String HOLOGRAM_TAG = "miranpc_hologram_";
    private static final String SKIN_TAG = "miranpc_skin_";
    private static final String INTERACTION_TAG = "miranpc_interaction_";
    private final MiraNPCPlugin plugin;
    private final NpcService npcs;
    private final NpcExtensionService extensions;
    private Method placeholderMethod;
    private boolean placeholderLookupAttempted;

    public NpcDisplayService(MiraNPCPlugin plugin, NpcService npcs, NpcExtensionService extensions) {
        this.plugin = plugin;
        this.npcs = npcs;
        this.extensions = extensions;
    }

    public void refresh() {
        cleanupOrphans();
        for (World world : Bukkit.getWorlds()) {
            for (Villager villager : world.getEntitiesByClass(Villager.class)) {
                if (!npcs.isNpc(villager)) continue;
                NpcDefinition definition = npcs.definition(villager).orElse(null);
                if (definition == null) continue;
                String instance = npcs.instanceId(villager).orElse("");
                if (instance.isBlank()) continue;
                NpcExtensionService.Extended extended = extensions.get(definition.id());
                boolean visible = extensions.visible(extended);
                boolean hologramOnly = extended.mode() == NpcExtensionService.Mode.HOLOGRAM;
                boolean playerMode = extended.mode() == NpcExtensionService.Mode.PLAYER;
                villager.setInvisible(!visible || hologramOnly || playerMode);
                villager.setCustomNameVisible(visible && !hologramOnly && !playerMode && extended.lines().isEmpty() && extended.rotationFrames().isEmpty());
                updateHolograms(villager, definition, extended, instance, visible);
                if (hologramOnly && visible) updateInteraction(villager, definition, instance);
                else removeInteraction(world, instance);
                if (playerMode && visible) updateCitizensSkin(villager, definition, extended, instance);
                else removeSkinEntity(world, instance);
            }
        }
    }

    private void updateHolograms(Villager anchor, NpcDefinition definition, NpcExtensionService.Extended extended, String instance, boolean visible) {
        List<String> lines = new ArrayList<>(extensions.displayLines(extended, System.currentTimeMillis()));
        if (lines.isEmpty() && (extended.mode() == NpcExtensionService.Mode.HOLOGRAM || extended.mode() == NpcExtensionService.Mode.PLAYER)) lines.add(definition.displayName());
        if (!visible || lines.isEmpty()) {
            removeHolograms(anchor.getWorld(), instance);
            return;
        }
        List<TextDisplay> existing = holograms(anchor.getWorld(), instance);
        while (existing.size() > lines.size()) existing.removeLast().remove();
        double baseHeight = extended.mode() == NpcExtensionService.Mode.HOLOGRAM ? 1.2D : 2.45D;
        double spacing = Math.max(0.20D, plugin.getConfig().getDouble("settings.hologram-line-spacing", 0.28D));
        for (int i = 0; i < lines.size(); i++) {
            TextDisplay display;
            if (i < existing.size()) display = existing.get(i);
            else {
                display = anchor.getWorld().spawn(anchor.getLocation(), TextDisplay.class);
                display.addScoreboardTag(HOLOGRAM_TAG + instance);
                display.setBillboard(Display.Billboard.CENTER);
                display.setSeeThrough(true);
                display.setShadowed(true);
                display.setPersistent(true);
                existing.add(display);
            }
            Location loc = anchor.getLocation().clone().add(0, baseHeight + ((lines.size() - 1 - i) * spacing), 0);
            display.teleport(loc);
            display.text(TextUtil.component(resolve(lines.get(i))));
            Transformation transformation = display.getTransformation();
            transformation.getScale().set(new Vector3f(1f, 1f, 1f));
            display.setTransformation(transformation);
        }
    }

    private List<TextDisplay> holograms(World world, String instance) {
        List<TextDisplay> list = new ArrayList<>();
        String tag = HOLOGRAM_TAG + instance;
        for (TextDisplay display : world.getEntitiesByClass(TextDisplay.class)) if (display.getScoreboardTags().contains(tag)) list.add(display);
        list.sort(Comparator.comparingDouble(Entity::getY).reversed());
        return list;
    }

    private void removeHolograms(World world, String instance) { for (TextDisplay display : holograms(world, instance)) display.remove(); }

    private void updateCitizensSkin(Villager anchor, NpcDefinition definition, NpcExtensionService.Extended extended, String instance) {
        if (!Bukkit.getPluginManager().isPluginEnabled("Citizens")) {
            removeSkinEntity(anchor.getWorld(), instance);
            if (!extended.skin().isBlank()) plugin.getLogger().fine("NPC " + definition.id() + " requests PLAYER mode but Citizens is not installed.");
            return;
        }
        Entity existing = skinEntity(anchor.getWorld(), instance);
        if (existing != null && existing.isValid()) {
            existing.teleport(anchor.getLocation());
            npcs.tagExternalEntity(existing, definition.id(), instance);
            applyCitizensSkin(existing, extended.skin());
            return;
        }
        try {
            Class<?> citizens = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Object registry = citizens.getMethod("getNPCRegistry").invoke(null);
            Class<?> registryClass = Class.forName("net.citizensnpcs.api.npc.NPCRegistry");
            Object npc = registryClass.getMethod("createNPC", org.bukkit.entity.EntityType.class, String.class)
                    .invoke(registry, org.bukkit.entity.EntityType.PLAYER, stripColors(resolve(definition.displayName())));
            Class<?> npcClass = Class.forName("net.citizensnpcs.api.npc.NPC");
            npcClass.getMethod("spawn", Location.class).invoke(npc, anchor.getLocation());
            if (!extended.skin().isBlank()) {
                Class<?> skinTrait = Class.forName("net.citizensnpcs.trait.SkinTrait");
                Object trait = npcClass.getMethod("getOrAddTrait", Class.class).invoke(npc, skinTrait);
                try { skinTrait.getMethod("setSkinName", String.class, boolean.class).invoke(trait, extended.skin(), true); }
                catch (NoSuchMethodException ex) { skinTrait.getMethod("setSkinName", String.class).invoke(trait, extended.skin()); }
            }
            Object entity = npcClass.getMethod("getEntity").invoke(npc);
            if (entity instanceof Entity bukkitEntity) {
                bukkitEntity.addScoreboardTag(SKIN_TAG + instance);
                npcs.tagExternalEntity(bukkitEntity, definition.id(), instance);
            }
        } catch (Throwable ex) {
            plugin.getLogger().warning("Could not create Citizens player NPC for " + definition.id() + ": " + ex.getMessage());
        }
    }

    private void updateInteraction(Villager anchor, NpcDefinition definition, String instance) {
        Interaction interaction = interactionEntity(anchor.getWorld(), instance);
        if (interaction == null || !interaction.isValid()) {
            interaction = anchor.getWorld().spawn(anchor.getLocation().clone().add(0, 0.9D, 0), Interaction.class);
            interaction.addScoreboardTag(INTERACTION_TAG + instance);
            interaction.setPersistent(true);
            interaction.setResponsive(true);
            interaction.setInteractionWidth((float) Math.max(0.2D, plugin.getConfig().getDouble("settings.hologram-hitbox-width", 1.0D)));
            interaction.setInteractionHeight((float) Math.max(0.2D, plugin.getConfig().getDouble("settings.hologram-hitbox-height", 2.0D)));
            npcs.tagExternalEntity(interaction, definition.id(), instance);
        } else {
            interaction.teleport(anchor.getLocation().clone().add(0, 0.9D, 0));
            npcs.tagExternalEntity(interaction, definition.id(), instance);
        }
    }

    private Interaction interactionEntity(World world, String instance) {
        String tag = INTERACTION_TAG + instance;
        for (Interaction interaction : world.getEntitiesByClass(Interaction.class)) {
            if (interaction.getScoreboardTags().contains(tag)) return interaction;
        }
        return null;
    }

    private void removeInteraction(World world, String instance) {
        Interaction interaction = interactionEntity(world, instance);
        if (interaction != null) interaction.remove();
    }

    private void applyCitizensSkin(Entity entity, String skin) {
        if (entity == null || skin == null || skin.isBlank()) return;
        try {
            Class<?> citizens = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Object registry = citizens.getMethod("getNPCRegistry").invoke(null);
            Class<?> registryClass = Class.forName("net.citizensnpcs.api.npc.NPCRegistry");
            Object npc = registryClass.getMethod("getNPC", Entity.class).invoke(registry, entity);
            if (npc == null) return;
            Class<?> npcClass = Class.forName("net.citizensnpcs.api.npc.NPC");
            Class<?> skinTrait = Class.forName("net.citizensnpcs.trait.SkinTrait");
            Object trait = npcClass.getMethod("getOrAddTrait", Class.class).invoke(npc, skinTrait);
            try { skinTrait.getMethod("setSkinName", String.class, boolean.class).invoke(trait, skin, true); }
            catch (NoSuchMethodException ex) { skinTrait.getMethod("setSkinName", String.class).invoke(trait, skin); }
        } catch (Throwable ignored) {
        }
    }

    private Entity skinEntity(World world, String instance) {
        String tag = SKIN_TAG + instance;
        for (Entity entity : world.getEntities()) if (entity.getScoreboardTags().contains(tag)) return entity;
        return null;
    }
    private void removeSkinEntity(World world, String instance) {
        Entity entity = skinEntity(world, instance);
        if (entity == null) return;
        try {
            Class<?> citizens = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Object registry = citizens.getMethod("getNPCRegistry").invoke(null);
            Class<?> registryClass = Class.forName("net.citizensnpcs.api.npc.NPCRegistry");
            Object npc = registryClass.getMethod("getNPC", Entity.class).invoke(registry, entity);
            if (npc != null) {
                Class<?> npcClass = Class.forName("net.citizensnpcs.api.npc.NPC");
                npcClass.getMethod("destroy").invoke(npc);
                return;
            }
        } catch (Throwable ignored) {
        }
        entity.remove();
    }

    private void cleanupOrphans() {
        Set<String> live = new HashSet<>();
        for (World world : Bukkit.getWorlds()) for (Villager villager : world.getEntitiesByClass(Villager.class)) if (npcs.isNpc(villager)) npcs.instanceId(villager).ifPresent(live::add);
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : new ArrayList<>(world.getEntities())) {
                for (String tag : entity.getScoreboardTags()) {
                    if (tag.startsWith(HOLOGRAM_TAG) && !live.contains(tag.substring(HOLOGRAM_TAG.length()))) entity.remove();
                    if (tag.startsWith(SKIN_TAG) && !live.contains(tag.substring(SKIN_TAG.length()))) removeSkinEntity(world, tag.substring(SKIN_TAG.length()));
                    if (tag.startsWith(INTERACTION_TAG) && !live.contains(tag.substring(INTERACTION_TAG.length()))) entity.remove();
                }
            }
        }
    }

    public String resolve(String raw) {
        if (raw == null || raw.isBlank() || !raw.contains("%")) return raw == null ? "" : raw;
        Method method = placeholderMethod();
        if (method == null) return raw;
        try {
            Object result = method.invoke(null, (org.bukkit.OfflinePlayer) null, raw);
            return result instanceof String value ? value : raw;
        } catch (ReflectiveOperationException ex) { return raw; }
    }

    private Method placeholderMethod() {
        if (placeholderLookupAttempted) return placeholderMethod;
        placeholderLookupAttempted = true;
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return null;
        try {
            Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            placeholderMethod = papi.getMethod("setPlaceholders", org.bukkit.OfflinePlayer.class, String.class);
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().warning("PlaceholderAPI is installed but MiraNPC could not access it: " + ex.getMessage());
        }
        return placeholderMethod;
    }

    private String stripColors(String raw) { return raw == null ? "NPC" : raw.replaceAll("(?i)&[0-9A-FK-ORX]", ""); }
}
