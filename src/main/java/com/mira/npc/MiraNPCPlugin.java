package com.mira.npc;

import com.mira.npc.command.MnpcCommand;
import com.mira.npc.gui.NpcGuiService;
import com.mira.npc.listener.NpcGuiListener;
import com.mira.npc.listener.NpcInteractionListener;
import com.mira.npc.service.DynamicNpcNameService;
import com.mira.npc.service.NpcDisplayService;
import com.mira.npc.service.NpcExtensionService;
import com.mira.npc.service.NpcService;
import com.mira.npc.util.TextUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class MiraNPCPlugin extends JavaPlugin {
    private static final String CHAT_PREFIX = "&5&lMira &8>> &r";

    private NpcService npcService;
    private NpcExtensionService extensionService;
    private NpcDisplayService displayService;
    private NpcGuiService guiService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        npcService = new NpcService(this);
        extensionService = new NpcExtensionService(this);
        displayService = new NpcDisplayService(this, npcService, extensionService);
        guiService = new NpcGuiService(this, npcService, extensionService);
        DynamicNpcNameService dynamicNames = new DynamicNpcNameService(this, npcService);

        getServer().getPluginManager().registerEvents(new NpcGuiListener(this, guiService), this);
        getServer().getPluginManager().registerEvents(new NpcInteractionListener(this, npcService), this);

        MnpcCommand command = new MnpcCommand(this, npcService, guiService, extensionService, displayService);
        PluginCommand mnpc = getCommand("mnpc");
        if (mnpc != null) {
            mnpc.setExecutor(command);
            mnpc.setTabCompleter(command);
        }

        getServer().getScheduler().runTask(this, () -> {
            npcService.restoreAll();
            displayService.refresh();
        });
        long ticks = Math.max(1L, getConfig().getLong("settings.enforce-position-ticks", 20L));
        getServer().getScheduler().runTaskTimer(this, npcService::enforcePositions, ticks, ticks);
        long dynamicNameTicks = Math.max(20L, getConfig().getLong("settings.dynamic-name-refresh-ticks", 100L));
        getServer().getScheduler().runTaskTimer(this, dynamicNames::refresh, dynamicNameTicks, dynamicNameTicks);
        long displayTicks = Math.max(10L, getConfig().getLong("settings.display-refresh-ticks", 40L));
        getServer().getScheduler().runTaskTimer(this, displayService::refresh, displayTicks, displayTicks);

        getLogger().info("MiraNPC v" + getPluginMeta().getVersion() + " enabled with villager, hologram and native Paper mannequin PLAYER modes.");
    }

    public NpcExtensionService extensions() { return extensionService; }
    public NpcDisplayService displays() { return displayService; }

    public void reloadAll() {
        reloadConfig();
        npcService.reload();
        extensionService.reload();
        npcService.restoreAll();
        displayService.refresh();
    }

    public String message(String key) {
        return getConfig().getString("messages." + key, "&cMissing message: " + key);
    }

    public void msg(CommandSender sender, String text) {
        sender.sendMessage(TextUtil.component(CHAT_PREFIX + text));
    }
}
