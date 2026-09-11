package com.mira.npc.listener;

import com.mira.npc.MiraNPCPlugin;
import com.mira.npc.gui.NpcGuiService;
import com.mira.npc.gui.NpcHolder;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class NpcGuiListener implements Listener {
    private final MiraNPCPlugin plugin;
    private final NpcGuiService gui;

    public NpcGuiListener(MiraNPCPlugin plugin, NpcGuiService gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof NpcHolder holder)) return;
        event.setCancelled(true);
        if (!player.hasPermission("miranpc.admin")) {
            player.closeInventory();
            return;
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        int slot = event.getRawSlot();
        switch (holder.type()) {
            case MAIN -> gui.clickMain(player, slot);
            case MANAGE -> gui.clickManage(player, slot);
            case EDITOR -> gui.clickEditor(player, slot);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!gui.isAwaiting(player.getUniqueId())) return;
        event.setCancelled(true);
        String input = PlainTextComponentSerializer.plainText().serialize(event.message());
        plugin.getServer().getScheduler().runTask(plugin, () -> gui.acceptChat(player, input));
    }
}
