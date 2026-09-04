package com.mira.npc.command;

import com.mira.npc.MiraNPCPlugin;
import com.mira.npc.gui.NpcGuiService;
import com.mira.npc.model.NpcDefinition;
import com.mira.npc.service.NpcDisplayService;
import com.mira.npc.service.NpcExtensionService;
import com.mira.npc.service.NpcService;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class MnpcCommand implements CommandExecutor, TabCompleter {
    private final MiraNPCPlugin plugin;
    private final NpcService service;
    private final NpcGuiService gui;
    private final NpcExtensionService extensions;
    private final NpcDisplayService displays;

    public MnpcCommand(MiraNPCPlugin plugin, NpcService service, NpcGuiService gui,
                       NpcExtensionService extensions, NpcDisplayService displays) {
        this.plugin = plugin;
        this.service = service;
        this.gui = gui;
        this.extensions = extensions;
        this.displays = displays;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.msg(sender, "&cMiraNPC admin commands are player-only.");
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
            case "mode" -> mode(player, args);
            case "skin" -> skin(player, args);
            case "lines" -> lines(player, args);
            case "action" -> action(player, args);
            case "rotation" -> rotation(player, args);
            case "frame" -> frame(player, args);
            case "state" -> state(player, args);
            case "status" -> status(player, args);
            case "reload" -> {
                plugin.reloadAll();
                plugin.msg(player, "&aMiraNPC reloaded.");
            }
            default -> plugin.msg(player, "&eUsage: /mnpc [place|remove|delete|mode|skin|lines|action|rotation|frame|state|status|reload]");

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
        if (entity == null || !service.removeInstance(entity)) {
            plugin.msg(player, "&cLook directly at a MiraNPC to remove it.");
            return;
        }
        plugin.msg(player, "&aRemoved placed NPC.");
    }

    private void mode(Player player, String[] args) {
        if (args.length < 3 || service.definition(args[1]).isEmpty()) {
            plugin.msg(player, "&eUsage: /mnpc mode <npc> <villager|hologram|player>");
            return;
        }
        try {
            NpcExtensionService.Mode mode = NpcExtensionService.Mode.valueOf(args[2].toUpperCase(Locale.ROOT));
            extensions.mode(args[1], mode);
            displays.refresh();
            plugin.msg(player, "&aNPC mode set to &f" + mode.name() + "&a.");
            if (mode == NpcExtensionService.Mode.PLAYER && !plugin.getServer().getPluginManager().isPluginEnabled("Citizens")) {
                plugin.msg(player, "&eCitizens is not installed, so PLAYER mode will remain unavailable until Citizens is present.");
            }
        } catch (IllegalArgumentException ex) {
            plugin.msg(player, "&cMode must be VILLAGER, HOLOGRAM or PLAYER.");
        }
    }

    private void skin(Player player, String[] args) {
        if (args.length < 3 || service.definition(args[1]).isEmpty()) {
            plugin.msg(player, "&eUsage: /mnpc skin <npc> <minecraftName|clear>");
            return;
        }
        extensions.skin(args[1], args[2].equalsIgnoreCase("clear") ? "" : args[2]);
        displays.refresh();
        plugin.msg(player, "&aNPC skin updated.");
    }

    private void lines(Player player, String[] args) {
        if (args.length < 3 || service.definition(args[1]).isEmpty()) {
            plugin.msg(player, "&eUsage: /mnpc lines <npc> <line1|line2|...|clear>");
            return;
        }
        String raw = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        List<String> lines = raw.equalsIgnoreCase("clear") ? List.of()
                : Arrays.stream(raw.split("\\|")).map(String::trim).filter(v -> !v.isBlank()).toList();
        extensions.lines(args[1], lines);
        displays.refresh();
        plugin.msg(player, "&aNPC floating text updated.");
    }

    private void action(Player player, String[] args) {
        if (args.length < 3) {
            plugin.msg(player, "&eUsage: /mnpc action <add|clear> <npc> [player|console] [command]");
            return;
        }
        String operation = args[1].toLowerCase(Locale.ROOT);
        String id = args[2];
        if (service.definition(id).isEmpty()) { plugin.msg(player, "&cUnknown NPC."); return; }
        if (operation.equals("clear")) {
            extensions.clearActions(id);
            plugin.msg(player, "&aNPC command chain cleared.");
            return;
        }
        if (!operation.equals("add") || args.length < 5) {
            plugin.msg(player, "&eUsage: /mnpc action add <npc> <player|console> <command>");
            return;
        }
        NpcDefinition.Executor executor;
        try { executor = NpcDefinition.Executor.valueOf(args[3].toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { plugin.msg(player, "&cExecutor must be PLAYER or CONSOLE."); return; }
        String command = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
        extensions.addAction(id, new NpcExtensionService.Action(command, executor));
        plugin.msg(player, "&aAdded command-chain action.");
    }

    private void rotation(Player player, String[] args) {
        if (args.length < 3 || service.definition(args[1]).isEmpty()) {
            plugin.msg(player, "&eUsage: /mnpc rotation <npc> <seconds|clear>");
            return;
        }
        if (args[2].equalsIgnoreCase("clear")) {
            extensions.clearRotation(args[1]);
            displays.refresh();
            plugin.msg(player, "&aRotation frames cleared.");
            return;
        }
        try {
            extensions.rotationSeconds(args[1], Long.parseLong(args[2]));
            displays.refresh();
            plugin.msg(player, "&aRotation interval updated.");
        } catch (NumberFormatException ex) {
            plugin.msg(player, "&cRotation seconds must be a number.");
        }
    }

    private void frame(Player player, String[] args) {
        if (args.length < 4 || service.definition(args[1]).isEmpty()) {
            plugin.msg(player, "&eUsage: /mnpc frame <npc> <number> <line1|line2|...>");
            return;
        }
        try {
            int number = Integer.parseInt(args[2]);
            String raw = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
            List<String> lines = Arrays.stream(raw.split("\\|")).map(String::trim).filter(v -> !v.isBlank()).toList();
            extensions.rotationFrame(args[1], number, lines);
            displays.refresh();
            plugin.msg(player, "&aRotation frame updated.");
        } catch (NumberFormatException ex) {
            plugin.msg(player, "&cFrame number must be numeric.");
        }
    }

    private void state(Player player, String[] args) {
        if (args.length < 4) {
            plugin.msg(player, "&eUsage: /mnpc state <set|remove> <npc> <state> ...");
            return;
        }
        String operation = args[1].toLowerCase(Locale.ROOT);
        String id = args[2];
        if (service.definition(id).isEmpty()) { plugin.msg(player, "&cUnknown NPC."); return; }
        if (operation.equals("remove")) {
            extensions.removeTimedState(id, args[3]);
            displays.refresh();
            plugin.msg(player, "&aTimed state removed.");
            return;
        }
        if (!operation.equals("set") || args.length < 8) {
            plugin.msg(player, "&eUsage: /mnpc state set <npc> <state> <HH:mm> <HH:mm> <true|false> <line1|line2|...>");
            return;
        }
        boolean visible = Boolean.parseBoolean(args[6]);
        String raw = String.join(" ", Arrays.copyOfRange(args, 7, args.length));
        List<String> lines = Arrays.stream(raw.split("\\|")).map(String::trim).filter(v -> !v.isBlank()).toList();
        extensions.setTimedState(id, args[3], args[4], args[5], visible, lines);
        displays.refresh();
        plugin.msg(player, "&aTimed NPC state updated.");
    }

    private void status(Player player, String[] args) {
        if (args.length < 2 || service.definition(args[1]).isEmpty()) {
            plugin.msg(player, "&eUsage: /mnpc status <npc>");
            return;
        }
        var ext = extensions.get(args[1]);
        plugin.msg(player, "&dNPC &f" + args[1] + " &7mode &f" + ext.mode().name()
                + " &7skin &f" + (ext.skin().isBlank() ? "None" : ext.skin()));
        plugin.msg(player, "&7Lines: &f" + ext.lines().size() + " &7frames: &f" + ext.rotationFrames().size()
                + " &7actions: &f" + ext.actions().size() + " &7states: &f" + ext.timedStates().size());
        plugin.msg(player, "&7Citizens player backend: " + (plugin.getServer().getPluginManager().isPluginEnabled("Citizens") ? "&aAVAILABLE" : "&cUNAVAILABLE"));
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
        if (args.length == 1) return filter(List.of("place", "remove", "delete", "mode", "skin", "lines", "action", "rotation", "frame", "state", "status", "reload"), args[0]);
        if (args.length == 2 && List.of("place","delete","mode","skin","lines","rotation","frame","status").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(service.definitions().stream().map(NpcDefinition::id).toList(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("mode")) return filter(List.of("villager","hologram","player"), args[2]);
        return List.of();
    }

    private static List<String> filter(List<String> values, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String value : values) if (value.toLowerCase(Locale.ROOT).startsWith(lower)) out.add(value);
        return out;
    }
}
