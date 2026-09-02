package com.mira.npc.gui;

import com.mira.npc.MiraNPCPlugin;
import com.mira.npc.model.NpcDefinition;
import com.mira.npc.service.NpcService;
import com.mira.npc.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class NpcGuiService {
    public enum Awaiting { CREATE_ID, NAME, COMMAND }

    public record EditState(String id, String name, String command, NpcDefinition.Executor executor) {}

    private final MiraNPCPlugin plugin;
    private final NpcService service;
    private final Map<UUID, EditState> editing = new ConcurrentHashMap<>();
    private final Map<UUID, Awaiting> awaiting = new ConcurrentHashMap<>();

    public NpcGuiService(MiraNPCPlugin plugin, NpcService service) {
        this.plugin = plugin;
        this.service = service;
    }

    public void openMain(Player player) {
        Inventory inv = inventory(new NpcHolder(NpcHolder.Type.MAIN, ""), 27, "&5Mira NPC");
        inv.setItem(11, button(Material.EMERALD, "&aCreate NPC", List.of("&7Create a new NPC definition")));
        inv.setItem(15, button(Material.BOOK, "&eManage NPCs", List.of("&7Edit existing NPC definitions")));
        player.openInventory(inv);
    }

    public void beginCreate(Player player) {
        awaiting.put(player.getUniqueId(), Awaiting.CREATE_ID);
        player.closeInventory();
        plugin.msg(player, "&eType an internal NPC ID in chat, for example &fshop&e.");
    }

    public void openManage(Player player) {
        Inventory inv = inventory(new NpcHolder(NpcHolder.Type.MANAGE, ""), 54, "&5Manage NPCs");
        int slot = 0;
        for (NpcDefinition definition : service.definitions()) {
            if (slot >= 45) break;
            inv.setItem(slot++, button(Material.VILLAGER_SPAWN_EGG, definition.displayName(), List.of(
                    "&7ID: &f" + definition.id(),
                    "&7Function: &f/" + definition.command(),
                    "&7Executor: &f" + definition.executor().name(),
                    "",
                    "&eClick to edit"
            )));
        }
        inv.setItem(49, button(Material.ARROW, "&cBack", List.of()));
        player.openInventory(inv);
    }

    public void openEditor(Player player, NpcDefinition definition) {
        EditState state = new EditState(definition.id(), definition.displayName(), definition.command(), definition.executor());
        editing.put(player.getUniqueId(), state);
        renderEditor(player, state);
    }

    private void renderEditor(Player player, EditState state) {
        Inventory inv = inventory(new NpcHolder(NpcHolder.Type.EDITOR, state.id()), 27, "&5Edit NPC: &f" + state.id());
        inv.setItem(10, button(Material.NAME_TAG, "&eSet Name", List.of("&7Current: " + state.name(), "&eClick then type the name in chat")));
        inv.setItem(12, button(Material.COMMAND_BLOCK, "&eSet Function", List.of("&7Current: &f/" + state.command(), "&eClick then type the command in chat")));
        inv.setItem(14, button(Material.LEVER, "&eExecutor: &f" + state.executor().name(), List.of("&7PLAYER runs it as the clicking player", "&7CONSOLE runs it as console", "&eClick to toggle")));
        inv.setItem(16, button(Material.LIME_CONCRETE, "&aSave NPC", List.of("&7Save this definition")));
        inv.setItem(22, button(Material.BARRIER, "&cCancel", List.of()));
        player.openInventory(inv);
    }

    public void clickMain(Player player, int slot) {
        if (slot == 11) beginCreate(player);
        else if (slot == 15) openManage(player);
    }

    public void clickManage(Player player, int slot) {
        if (slot == 49) {
            openMain(player);
            return;
        }
        if (slot < 0 || slot >= 45) return;
        int index = 0;
        for (NpcDefinition definition : service.definitions()) {
            if (index++ == slot) {
                openEditor(player, definition);
                return;
            }
        }
    }

    public void clickEditor(Player player, int slot) {
        EditState state = editing.get(player.getUniqueId());
        if (state == null) return;
        switch (slot) {
            case 10 -> beginInput(player, Awaiting.NAME, "&eType the NPC display name in chat. Legacy & colour codes are supported.");
            case 12 -> beginInput(player, Awaiting.COMMAND, "&eType the command/function in chat. A leading / is optional.");
            case 14 -> {
                NpcDefinition.Executor next = state.executor() == NpcDefinition.Executor.PLAYER ? NpcDefinition.Executor.CONSOLE : NpcDefinition.Executor.PLAYER;
                EditState updated = new EditState(state.id(), state.name(), state.command(), next);
                editing.put(player.getUniqueId(), updated);
                renderEditor(player, updated);
            }
            case 16 -> save(player);
            case 22 -> {
                editing.remove(player.getUniqueId());
                openMain(player);
            }
            default -> { }
        }
    }

    private void beginInput(Player player, Awaiting type, String message) {
        awaiting.put(player.getUniqueId(), type);
        player.closeInventory();
        plugin.msg(player, message);
    }

    public boolean isAwaiting(UUID uuid) {
        return awaiting.containsKey(uuid);
    }

    public void acceptChat(Player player, String message) {
        Awaiting type = awaiting.remove(player.getUniqueId());
        if (type == null) return;
        String value = message == null ? "" : message.trim();

        if (type == Awaiting.CREATE_ID) {
            String id = normalize(value);
            if (id.isBlank()) {
                plugin.msg(player, "&cNPC ID cannot be blank.");
                openMain(player);
                return;
            }
            if (service.definition(id).isPresent()) {
                plugin.msg(player, "&cAn NPC with that ID already exists.");
                openMain(player);
                return;
            }
            openEditor(player, new NpcDefinition(id, "&f" + value, "", NpcDefinition.Executor.PLAYER));
            return;
        }

        EditState state = editing.get(player.getUniqueId());
        if (state == null) {
            openMain(player);
            return;
        }

        if (type == Awaiting.NAME) {
            state = new EditState(state.id(), value, state.command(), state.executor());
        } else if (type == Awaiting.COMMAND) {
            state = new EditState(state.id(), state.name(), stripSlash(value), state.executor());
        }
        editing.put(player.getUniqueId(), state);
        renderEditor(player, state);
    }

    private void save(Player player) {
        EditState state = editing.get(player.getUniqueId());
        if (state == null) return;
        if (state.name().isBlank()) {
            plugin.msg(player, "&cNPC name cannot be blank.");
            return;
        }
        if (state.command().isBlank()) {
            plugin.msg(player, "&cNPC function/command cannot be blank.");
            return;
        }
        service.saveDefinition(new NpcDefinition(state.id(), state.name(), state.command(), state.executor()));
        editing.remove(player.getUniqueId());
        plugin.msg(player, plugin.message("saved").replace("%npc%", state.id()));
        openManage(player);
    }

    private static Inventory inventory(NpcHolder holder, int size, String title) {
        Inventory inv = Bukkit.createInventory(holder, size, TextUtil.component(title));
        holder.bind(inv);
        return inv;
    }

    private static ItemStack button(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextUtil.component(name));
        meta.lore(lore.stream().map(TextUtil::component).toList());
        item.setItemMeta(meta);
        return item;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace(' ', '_').replaceAll("[^a-z0-9_\-]", "");
    }

    private static String stripSlash(String value) {
        String out = value;
        while (out.startsWith("/")) out = out.substring(1);
        return out;
    }
}
