package com.mira.npc.service;

import com.mira.npc.MiraNPCPlugin;
import com.mira.npc.model.NpcDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class NpcExtensionService {
    public enum Mode { VILLAGER, HOLOGRAM, PLAYER }
    public record Action(String command, NpcDefinition.Executor executor) {}
    public record TimedState(String id, LocalTime start, LocalTime end, List<String> lines, List<Action> actions, boolean visible) {}
    public record Extended(String id, Mode mode, String skin, List<String> lines, List<List<String>> rotationFrames,
                           long rotationSeconds, List<Action> actions, List<TimedState> timedStates) {}

    private final MiraNPCPlugin plugin;
    private final File file;
    private final YamlConfiguration yaml;

    public NpcExtensionService(MiraNPCPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "npc-extensions.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized Extended get(String id) {
        String key = normalize(id);
        String base = "npcs." + key;
        Mode mode;
        try { mode = Mode.valueOf(yaml.getString(base + ".mode", "VILLAGER").toUpperCase(Locale.ROOT)); }
        catch (Exception ignored) { mode = Mode.VILLAGER; }
        String skin = yaml.getString(base + ".skin", "");
        List<String> lines = yaml.getStringList(base + ".lines");
        List<List<String>> frames = new ArrayList<>();
        ConfigurationSection frameRoot = yaml.getConfigurationSection(base + ".rotation.frames");
        if (frameRoot != null) {
            frameRoot.getKeys(false).stream().sorted(Comparator.comparingInt(this::safeInt)).forEach(k -> frames.add(frameRoot.getStringList(k)));
        }
        long seconds = Math.max(1L, yaml.getLong(base + ".rotation.seconds", 10L));
        List<Action> actions = readActions(base + ".actions");
        List<TimedState> states = new ArrayList<>();
        ConfigurationSection stateRoot = yaml.getConfigurationSection(base + ".states");
        if (stateRoot != null) {
            for (String stateId : stateRoot.getKeys(false)) {
                String s = base + ".states." + stateId;
                try {
                    LocalTime start = LocalTime.parse(yaml.getString(s + ".start", "00:00"), DateTimeFormatter.ofPattern("HH:mm"));
                    LocalTime end = LocalTime.parse(yaml.getString(s + ".end", "23:59"), DateTimeFormatter.ofPattern("HH:mm"));
                    states.add(new TimedState(stateId, start, end, yaml.getStringList(s + ".lines"), readActions(s + ".actions"), yaml.getBoolean(s + ".visible", true)));
                } catch (Exception ex) {
                    plugin.getLogger().warning("Skipping malformed timed NPC state " + key + "/" + stateId + ": " + ex.getMessage());
                }
            }
        }
        return new Extended(key, mode, skin, List.copyOf(lines), List.copyOf(frames), seconds, List.copyOf(actions), List.copyOf(states));
    }

    public synchronized void mode(String id, Mode mode) { yaml.set(path(id) + ".mode", mode.name()); save(); }
    public synchronized void skin(String id, String skin) { yaml.set(path(id) + ".skin", skin == null ? "" : skin); save(); }
    public synchronized void lines(String id, List<String> lines) { yaml.set(path(id) + ".lines", lines == null ? List.of() : lines); save(); }
    public synchronized void rotationSeconds(String id, long seconds) { yaml.set(path(id) + ".rotation.seconds", Math.max(1L, seconds)); save(); }
    public synchronized void rotationFrame(String id, int frame, List<String> lines) { yaml.set(path(id) + ".rotation.frames." + Math.max(1, frame), lines); save(); }
    public synchronized void clearRotation(String id) { yaml.set(path(id) + ".rotation", null); save(); }

    public synchronized void addAction(String id, Action action) {
        String base = path(id) + ".actions";
        int next = 1;
        ConfigurationSection root = yaml.getConfigurationSection(base);
        if (root != null) for (String key : root.getKeys(false)) next = Math.max(next, safeInt(key) + 1);
        yaml.set(base + "." + next + ".command", stripSlash(action.command()));
        yaml.set(base + "." + next + ".executor", action.executor().name());
        save();
    }
    public synchronized void clearActions(String id) { yaml.set(path(id) + ".actions", null); save(); }

    public synchronized void setTimedState(String id, String state, String start, String end, boolean visible, List<String> lines) {
        String base = path(id) + ".states." + normalize(state);
        yaml.set(base + ".start", start);
        yaml.set(base + ".end", end);
        yaml.set(base + ".visible", visible);
        yaml.set(base + ".lines", lines == null ? List.of() : lines);
        save();
    }
    public synchronized void removeTimedState(String id, String state) { yaml.set(path(id) + ".states." + normalize(state), null); save(); }

    public Optional<TimedState> activeState(Extended extended) {
        LocalTime now = LocalTime.now();
        for (TimedState state : extended.timedStates()) {
            boolean active = state.start().equals(state.end()) || (state.start().isBefore(state.end())
                    ? !now.isBefore(state.start()) && now.isBefore(state.end())
                    : !now.isBefore(state.start()) || now.isBefore(state.end()));
            if (active) return Optional.of(state);
        }
        return Optional.empty();
    }

    public List<String> displayLines(Extended extended, long nowMillis) {
        Optional<TimedState> state = activeState(extended);
        if (state.isPresent() && !state.get().lines().isEmpty()) return state.get().lines();
        if (!extended.rotationFrames().isEmpty()) {
            long window = Math.max(1L, extended.rotationSeconds()) * 1000L;
            int index = (int) ((nowMillis / window) % extended.rotationFrames().size());
            return extended.rotationFrames().get(index);
        }
        return extended.lines();
    }

    public List<Action> actions(Extended extended) {
        Optional<TimedState> state = activeState(extended);
        return state.isPresent() && !state.get().actions().isEmpty() ? state.get().actions() : extended.actions();
    }

    public boolean visible(Extended extended) { return activeState(extended).map(TimedState::visible).orElse(true); }

    private List<Action> readActions(String base) {
        ConfigurationSection root = yaml.getConfigurationSection(base);
        if (root == null) return List.of();
        List<String> keys = new ArrayList<>(root.getKeys(false));
        keys.sort(Comparator.comparingInt(this::safeInt));
        List<Action> out = new ArrayList<>();
        for (String key : keys) {
            String command = yaml.getString(base + "." + key + ".command", "");
            if (command.isBlank()) continue;
            NpcDefinition.Executor executor;
            try { executor = NpcDefinition.Executor.valueOf(yaml.getString(base + "." + key + ".executor", "PLAYER").toUpperCase(Locale.ROOT)); }
            catch (Exception ignored) { executor = NpcDefinition.Executor.PLAYER; }
            out.add(new Action(stripSlash(command), executor));
        }
        return out;
    }

    private String path(String id) { return "npcs." + normalize(id); }
    private int safeInt(String raw) { try { return Integer.parseInt(raw); } catch (Exception ignored) { return Integer.MAX_VALUE / 2; } }
    private String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace(' ', '_'); }
    private String stripSlash(String value) { String out = value == null ? "" : value.trim(); while (out.startsWith("/")) out = out.substring(1); return out; }
    private void save() { try { if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs(); yaml.save(file); } catch (IOException ex) { plugin.getLogger().warning("Could not save npc-extensions.yml: " + ex.getMessage()); } }
}
