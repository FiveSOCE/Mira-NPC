package com.mira.npc;

import com.mira.npc.command.MnpcCommand;
import com.mira.npc.gui.NpcGuiService;
import com.mira.npc.listener.NpcGuiListener;
import com.mira.npc.listener.NpcInteractionListener;
import com.mira.npc.service.NpcService;
import com.mira.npc.util.TextUtil;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class MiraNPCPlugin extends JavaPlugin {
    private NpcService npcService;
    private NpcGuiService guiService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        npcService = new NpcService(this);
        guiService = new NpcGuiService(this, npcService);

        getServer().getPluginManager().registerEvents(new NpcGuiListener(this, guiService), this);
        getServer().getPluginManager().registerEvents(new NpcInteractionListener(this, npcService), this);

        MnpcCommand command = new MnpcCommand(this, npcService, guiService);
        PluginCommand mnpc = getCommand("mnpc");
        if (mnpc != null) {
            mnpc.setExecutor(command);
            mnpc.setTabCompleter(command);
        }

        getServer().getScheduler().runTask(this, npcService::restoreAll);
        long ticks = Math.max(1L, getConfig().getLong("settings.enforce-position-ticks", 20L));
        getServer().getScheduler().runTaskTimer(this, npcService::enforcePositions, ticks, ticks);

        getLogger().info("MiraNPC v" + getPluginMeta().getVersion() + " enabled.");
    }

    public String message(String key) {
        return getConfig().getString("messages." + key, "&cMissing message: " + key);
    }

    public void msg(Player player, String text) {
        player.sendMessage(TextUtil.component(text));
    }
}
