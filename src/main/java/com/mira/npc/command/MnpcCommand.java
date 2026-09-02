package com.mira.npc.command;

import com.mira.npc.MiraNPCPlugin;
import com.mira.npc.gui.NpcGuiService;
import com.mira.npc.model.NpcDefinition;
import com.mira.npc.service.NpcService;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MnpcCommand implements CommandExecutor, TabCompleter {
    private final MiraNPCPlugin plugin;
    private final NpcService service;
    private final NpcGuiService gui;

    public MnpcCommand(MiraNPCPlugin plugin, NpcService service, NpcGuiService gui) {
        this.plugin = plugin;
        this.service = service;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("MiraNPC admin commands are player-only.");
            return true;
        }
        if (!player.hasPermission("miranpc.admin")) {
            plugin.msg(player, plugin.message("no-permission"));
            return true;
        }

        if (args.length == 0) {
            gui.openMain(player);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "place" -> place(player, args);
            case "remove" -> remove(player);
            case "delete" -> delete(player, args);
            case "reload" -> {
                service.reload();
                service.restoreAll();
                plugin.msg(player, "&aMiraNPC reloaded.");
            }
            default -> plugin.msg(player, "&eUsage: /mnpc [place <npc>|remove|delete <npc>|reload]");
        }
        return true;
    }

    private void place(Player player, String[] args) {
        if (args.length < 2) {
            plugin.msg(player, "&eUsage: /mnpc place <NPC>");
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (service.definition(id).isEmpty()) {
            plugin.msg(player, plugin.message("unknown-npc").replace("%npc%", id));
            return;
        }
        int range = Math.max(1, plugin.getConfig().getInt("settings.placement-range", 8));
        Block block = player.getTargetBlockExact(range, FluidCollisionMode.NEVER);
        if (block == null) {
            plugin.msg(player, plugin.message("no-target-block"));
            return;
        }
        service.place(id, block.getLocation(), player.getLocation().getYaw() + 180f);
        plugin.msg(player, plugin.message("placed").replace("%npc%", id));
    }

    private void remove(Player player) {
        int range = Math.max(1, plugin.getConfig().getInt("settings.placement-range", 8));
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(), player.getEyeLocation().getDirection(), range,
                entity -> service.isNpc(entity)
        );
        Entity entity = result == null ? null : result.getHitEntity();
        if (!(entity instanceof Villager villager) || !service.removeInstance(villager)) {
            plugin.msg(player, "&cLook directly at a MiraNPC to remove it.");
            return;
        }
        plugin.msg(player, "&aRemoved placed NPC.");
    }

    private void delete(Player player, String[] args) {
        if (args.length < 2) {
            plugin.msg(player, "&eUsage: /mnpc delete <NPC>");
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (service.definition(id).isEmpty()) {
            plugin.msg(player, plugin.message("unknown-npc").replace("%npc%", id));
            return;
        }
        service.deleteDefinition(id);
        plugin.msg(player, plugin.message("deleted").replace("%npc%", id));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("miranpc.admin")) return List.of();
        if (args.length == 1) return filter(List.of("place", "remove", "delete", "reload"), args[0]);
        if (args.length == 2 && (args[0].equalsIgnoreCase("place") || args[0].equalsIgnoreCase("delete"))) {
            return filter(service.definitions().stream().map(NpcDefinition::id).toList(), args[1]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> values, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String value : values) if (value.toLowerCase(Locale.ROOT).startsWith(lower)) out.add(value);
        return out;
    }
}
