package com.mira.npc.service;

import com.mira.npc.MiraNPCPlugin;
import com.mira.npc.model.NpcDefinition;
import com.mira.npc.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Villager;

import java.lang.reflect.Method;

public final class DynamicNpcNameService {
    private final MiraNPCPlugin plugin;
    private final NpcService npcs;
    private Method placeholderMethod;
    private boolean placeholderLookupAttempted;

    public DynamicNpcNameService(MiraNPCPlugin plugin, NpcService npcs) {
        this.plugin = plugin;
        this.npcs = npcs;
    }

    public void refresh() {
        for (World world : Bukkit.getWorlds()) {
            for (Villager villager : world.getEntitiesByClass(Villager.class)) {
                if (!npcs.isNpc(villager)) continue;
                NpcDefinition definition = npcs.definition(villager).orElse(null);
                if (definition == null) continue;
                String resolved = resolve(definition.displayName());
                villager.customName(TextUtil.component(resolved));
                villager.setCustomNameVisible(true);
            }
        }
    }

    private String resolve(String raw) {
        if (raw == null || raw.isBlank() || !raw.contains("%")) return raw == null ? "" : raw;
        Method method = placeholderMethod();
        if (method == null) return raw;
        try {
            Object result = method.invoke(null, (OfflinePlayer) null, raw);
            return result instanceof String value ? value : raw;
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().fine("Could not resolve NPC placeholders: " + ex.getMessage());
            return raw;
        }
    }

    private Method placeholderMethod() {
        if (placeholderLookupAttempted) return placeholderMethod;
        placeholderLookupAttempted = true;
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return null;
        try {
            Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            placeholderMethod = papi.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().warning("PlaceholderAPI is installed but MiraNPC could not access it: " + ex.getMessage());
        }
        return placeholderMethod;
    }
}
