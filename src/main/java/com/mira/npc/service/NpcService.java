package com.mira.npc.service;

import com.mira.npc.MiraNPCPlugin;
import com.mira.npc.model.NpcDefinition;
import com.mira.npc.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class NpcService {
    private final MiraNPCPlugin plugin;
    private final File definitionsFile;
    private final File placedFile;
    private final NamespacedKey npcIdKey;
    private final NamespacedKey instanceIdKey;
    private YamlConfiguration definitionsYaml;
    private YamlConfiguration placedYaml;
    private final Map<String, NpcDefinition> definitions = new LinkedHashMap<>();

    public NpcService(MiraNPCPlugin plugin) {
        this.plugin = plugin;
        this.definitionsFile = new File(plugin.getDataFolder(), "npcs.yml");
        this.placedFile = new File(plugin.getDataFolder(), "placed.yml");
        this.npcIdKey = new NamespacedKey(plugin, "npc_id");
        this.instanceIdKey = new NamespacedKey(plugin, "instance_id");
        reload();
    }

    public void reload() {
        definitionsYaml = YamlConfiguration.loadConfiguration(definitionsFile);
        placedYaml = YamlConfiguration.loadConfiguration(placedFile);
        definitions.clear();
        ConfigurationSection section = definitionsYaml.getConfigurationSection("npcs");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                String base = "npcs." + id;
                String name = definitionsYaml.getString(base + ".name", id);
                String command = definitionsYaml.getString(base + ".command", "");
                String rawExecutor = definitionsYaml.getString(base + ".executor", "PLAYER");
                NpcDefinition.Executor executor;
                try { executor = NpcDefinition.Executor.valueOf(rawExecutor.toUpperCase(Locale.ROOT)); }
                catch (IllegalArgumentException ex) { executor = NpcDefinition.Executor.PLAYER; }
                definitions.put(normalize(id), new NpcDefinition(normalize(id), name, command, executor));
            }
        }
    }

    public Collection<NpcDefinition> definitions() { return Collections.unmodifiableCollection(definitions.values()); }
    public Optional<NpcDefinition> definition(String id) { return Optional.ofNullable(definitions.get(normalize(id))); }

    public void saveDefinition(NpcDefinition definition) {
        String id = normalize(definition.id());
        NpcDefinition normalized = new NpcDefinition(id, definition.displayName(), stripLeadingSlash(definition.command()), definition.executor());
        definitions.put(id, normalized);
        String base = "npcs." + id;
        definitionsYaml.set(base + ".name", normalized.displayName());
        definitionsYaml.set(base + ".command", normalized.command());
        definitionsYaml.set(base + ".executor", normalized.executor().name());
        save(definitionsYaml, definitionsFile);
        refreshPlaced(id);
    }

    public void deleteDefinition(String id) {
        String normalized = normalize(id);
        definitions.remove(normalized);
        definitionsYaml.set("npcs." + normalized, null);
        save(definitionsYaml, definitionsFile);
        removePlacedByDefinition(normalized);
    }

    public Villager place(String npcId, Location blockTop, float yaw) {
        NpcDefinition definition = definitions.get(normalize(npcId));
        if (definition == null) return null;
        Location spawn = blockTop.clone().add(0.5, 1.0, 0.5);
        spawn.setYaw(yaw);
        spawn.setPitch(0f);
        Villager villager = spawn.getWorld().spawn(spawn, Villager.class, entity -> configureEntity(entity, definition));
        String instanceId = UUID.randomUUID().toString();
        villager.getPersistentDataContainer().set(instanceIdKey, PersistentDataType.STRING, instanceId);
        persistInstance(instanceId, definition.id(), villager.getUniqueId(), spawn);
        return villager;
    }

    public boolean removeInstance(Entity entity) {
        String instanceId = entity.getPersistentDataContainer().get(instanceIdKey, PersistentDataType.STRING);
        if (instanceId == null) return false;
        placedYaml.set("instances." + instanceId, null);
        save(placedYaml, placedFile);
        for (World world : Bukkit.getWorlds()) {
            for (Entity candidate : new ArrayList<>(world.getEntities())) {
                String candidateInstance = candidate.getPersistentDataContainer().get(instanceIdKey, PersistentDataType.STRING);
                if (instanceId.equals(candidateInstance)) candidate.remove();
                if (candidate.getScoreboardTags().contains("miranpc_hologram_" + instanceId) || candidate.getScoreboardTags().contains("miranpc_skin_" + instanceId)) candidate.remove();
            }
        }
        return true;
    }

    public boolean isNpc(Entity entity) {
        return entity != null && entity.getPersistentDataContainer().has(npcIdKey, PersistentDataType.STRING);
    }

    public Optional<NpcDefinition> definition(Entity entity) {
        String id = entity.getPersistentDataContainer().get(npcIdKey, PersistentDataType.STRING);
        return id == null ? Optional.empty() : definition(id);
    }

    public Optional<String> instanceId(Entity entity) {
        if (entity == null) return Optional.empty();
        return Optional.ofNullable(entity.getPersistentDataContainer().get(instanceIdKey, PersistentDataType.STRING));
    }

    public void tagExternalEntity(Entity entity, String npcId, String instanceId) {
        if (entity == null) return;
        entity.getPersistentDataContainer().set(npcIdKey, PersistentDataType.STRING, normalize(npcId));
        entity.getPersistentDataContainer().set(instanceIdKey, PersistentDataType.STRING, instanceId);
    }

    public boolean execute(Player player, NpcDefinition definition) {
        NpcExtensionService.Extended extended = plugin.extensions().get(definition.id());
        List<NpcExtensionService.Action> chain = plugin.extensions().actions(extended);
        if (!chain.isEmpty()) {
            boolean any = false;
            for (NpcExtensionService.Action action : chain) {
                String command = placeholders(action.command(), player);
                if (command.isBlank()) continue;
                any |= action.executor() == NpcDefinition.Executor.CONSOLE
                        ? Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
                        : Bukkit.dispatchCommand(player, command);
            }
            return any;
        }
        String command = placeholders(definition.command(), player);
        if (command.isBlank()) return false;
        return definition.executor() == NpcDefinition.Executor.CONSOLE
                ? Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
                : Bukkit.dispatchCommand(player, command);
    }

    private String placeholders(String command, Player player) {
        return stripLeadingSlash(command)
                .replace("%player%", player.getName())
                .replace("%username%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString());
    }

    public void restoreAll() {
        ConfigurationSection section = placedYaml.getConfigurationSection("instances");
        if (section == null) return;
        for (String instanceId : new ArrayList<>(section.getKeys(false))) {
            String base = "instances." + instanceId;
            String npcId = placedYaml.getString(base + ".npc");
            NpcDefinition definition = definitions.get(normalize(npcId));
            if (definition == null) continue;
            World world = Bukkit.getWorld(placedYaml.getString(base + ".world", ""));
            if (world == null) continue;
            Location location = new Location(world, placedYaml.getDouble(base + ".x"), placedYaml.getDouble(base + ".y"), placedYaml.getDouble(base + ".z"), (float) placedYaml.getDouble(base + ".yaw"), 0f);
            Villager entity = findInstance(instanceId).orElse(null);
            if (entity == null || !entity.isValid()) {
                entity = world.spawn(location, Villager.class, villager -> configureEntity(villager, definition));
                entity.getPersistentDataContainer().set(instanceIdKey, PersistentDataType.STRING, instanceId);
                placedYaml.set(base + ".entity", entity.getUniqueId().toString());
            } else {
                configureEntity(entity, definition);
                entity.teleport(location);
            }
        }
        save(placedYaml, placedFile);
    }

    public void enforcePositions() {
        ConfigurationSection section = placedYaml.getConfigurationSection("instances");
        if (section == null) return;
        for (String instanceId : section.getKeys(false)) {
            String base = "instances." + instanceId;
            Villager villager = findInstance(instanceId).orElse(null);
            if (villager == null || !villager.isValid()) continue;
            World world = Bukkit.getWorld(placedYaml.getString(base + ".world", ""));
            if (world == null) continue;
            Location target = new Location(world, placedYaml.getDouble(base + ".x"), placedYaml.getDouble(base + ".y"), placedYaml.getDouble(base + ".z"), (float) placedYaml.getDouble(base + ".yaw"), 0f);
            if (villager.getLocation().distanceSquared(target) > 0.0001) villager.teleport(target);
            villager.setVelocity(villager.getVelocity().zero());
        }
    }

    private void refreshPlaced(String npcId) {
        NpcDefinition definition = definitions.get(npcId);
        if (definition == null) return;
        for (World world : Bukkit.getWorlds()) for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            String id = villager.getPersistentDataContainer().get(npcIdKey, PersistentDataType.STRING);
            if (npcId.equals(id)) configureEntity(villager, definition);
        }
    }

    private void removePlacedByDefinition(String npcId) {
        ConfigurationSection section = placedYaml.getConfigurationSection("instances");
        if (section == null) return;
        for (String instanceId : new ArrayList<>(section.getKeys(false))) {
            String base = "instances." + instanceId;
            if (!npcId.equals(normalize(placedYaml.getString(base + ".npc")))) continue;
            findInstance(instanceId).ifPresent(this::removeInstance);
            placedYaml.set(base, null);
        }
        save(placedYaml, placedFile);
    }

    private Optional<Villager> findInstance(String instanceId) {
        for (World world : Bukkit.getWorlds()) for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            String id = villager.getPersistentDataContainer().get(instanceIdKey, PersistentDataType.STRING);
            if (instanceId.equals(id)) return Optional.of(villager);
        }
        return Optional.empty();
    }

    private void configureEntity(Villager villager, NpcDefinition definition) {
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setCollidable(false);
        villager.setSilent(true);
        villager.setPersistent(true);
        villager.setRemoveWhenFarAway(false);
        villager.setCanPickupItems(false);
        villager.setAdult();
        villager.setProfession(Villager.Profession.NONE);
        villager.customName(TextUtil.component(definition.displayName()));
        villager.setCustomNameVisible(true);
        villager.getPersistentDataContainer().set(npcIdKey, PersistentDataType.STRING, definition.id());
    }

    private void persistInstance(String instanceId, String npcId, UUID entityId, Location location) {
        String base = "instances." + instanceId;
        placedYaml.set(base + ".npc", npcId);
        placedYaml.set(base + ".entity", entityId.toString());
        placedYaml.set(base + ".world", location.getWorld().getName());
        placedYaml.set(base + ".x", location.getX());
        placedYaml.set(base + ".y", location.getY());
        placedYaml.set(base + ".z", location.getZ());
        placedYaml.set(base + ".yaw", location.getYaw());
        save(placedYaml, placedFile);
    }

    private static String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace(' ', '_'); }
    private static String stripLeadingSlash(String command) { String value = command == null ? "" : command.trim(); while (value.startsWith("/")) value = value.substring(1); return value; }

    private void save(YamlConfiguration yaml, File file) {
        try { if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs(); yaml.save(file); }
        catch (IOException ex) { plugin.getLogger().severe("Failed to save " + file.getName() + ": " + ex.getMessage()); }
    }
}
