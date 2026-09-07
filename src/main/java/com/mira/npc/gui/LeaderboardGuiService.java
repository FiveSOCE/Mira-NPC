package com.mira.npc.gui;

import com.mira.npc.MiraNPCPlugin;
import com.mira.npc.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;

public final class LeaderboardGuiService {
    public enum Type { FTOP, BALTOP }

    private final MiraNPCPlugin plugin;

    public LeaderboardGuiService(MiraNPCPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean open(Player player, Type type) {
        return type == Type.FTOP ? openFTop(player) : openBalTop(player);
    }

    private boolean openFTop(Player player) {
        var factionsPlugin = Bukkit.getPluginManager().getPlugin("MiraFactions");
        if (factionsPlugin == null || !factionsPlugin.isEnabled()) {
            plugin.msg(player, "&cMiraFactions is not available.");
            return false;
        }

        try {
            Object service = factionsPlugin.getClass().getMethod("factions").invoke(factionsPlugin);
            Object landValue = factionsPlugin.getClass().getMethod("landValue").invoke(factionsPlugin);
            Collection<?> factions = (Collection<?>) service.getClass().getMethod("all").invoke(service);

            List<FactionRow> rows = new ArrayList<>();
            for (Object faction : factions) {
                String name = Objects.toString(faction.getClass().getMethod("name").invoke(faction), "Faction");
                double bank = ((Number) faction.getClass().getMethod("bankBalance").invoke(faction)).doubleValue();
                double land = ((Number) landValue.getClass().getMethod("value", faction.getClass()).invoke(landValue, faction)).doubleValue();
                int members = ((Map<?, ?>) faction.getClass().getMethod("members").invoke(faction)).size();
                int claims = ((Set<?>) faction.getClass().getMethod("claims").invoke(faction)).size();
                double power = ((Number) service.getClass().getMethod("factionPower", faction.getClass()).invoke(service, faction)).doubleValue();
                rows.add(new FactionRow(name, land, bank, members, claims, power));
            }
            rows.sort(Comparator.comparingDouble(FactionRow::total).reversed().thenComparing(FactionRow::name, String.CASE_INSENSITIVE_ORDER));

            BoardHolder holder = new BoardHolder(Type.FTOP);
            Inventory inv = Bukkit.createInventory(holder, 27, TextUtil.component("&5&lFaction Top 10"));
            holder.bind(inv);
            fill(inv);

            int[] slots = {4, 10, 11, 12, 13, 14, 15, 16, 20, 22};
            for (int i = 0; i < Math.min(10, rows.size()); i++) {
                FactionRow row = rows.get(i);
                Material icon = switch (i) {
                    case 0 -> Material.NETHERITE_BLOCK;
                    case 1 -> Material.DIAMOND_BLOCK;
                    case 2 -> Material.GOLD_BLOCK;
                    default -> Material.IRON_BLOCK;
                };
                ItemStack item = button(icon, "&d#" + (i + 1) + " &f" + row.name(), List.of(
                        "&7Total Value: &a$" + money(row.total()),
                        "&7Land Assets: &f$" + money(row.land()),
                        "&7Faction Bank: &f$" + money(row.bank()),
                        "&7Members: &f" + row.members(),
                        "&7Claims: &f" + row.claims(),
                        "&7Power: &f" + String.format(Locale.US, "%.1f", row.power()),
                        "",
                        "&8/f info " + row.name(),
                        "&eClick to view faction info"
                ));
                ItemMeta meta = item.getItemMeta();
                meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "ftop_faction"),
                        org.bukkit.persistence.PersistentDataType.STRING, row.name());
                item.setItemMeta(meta);
                inv.setItem(slots[i], item);
            }
            player.openInventory(inv);
            return true;
        } catch (ReflectiveOperationException | ClassCastException ex) {
            plugin.getLogger().warning("Could not build FTop NPC GUI: " + ex.getMessage());
            plugin.msg(player, "&cCould not load FTop right now.");
            return false;
        }
    }

    private boolean openBalTop(Player player) {
        Object economy = vaultEconomy();
        if (economy == null) {
            plugin.msg(player, "&cVault economy is not available.");
            return false;
        }

        try {
            Method balanceMethod = Arrays.stream(economy.getClass().getMethods())
                    .filter(m -> m.getName().equals("getBalance") && m.getParameterCount() == 1
                            && OfflinePlayer.class.isAssignableFrom(m.getParameterTypes()[0]))
                    .findFirst().orElse(null);
            if (balanceMethod == null) throw new NoSuchMethodException("Economy#getBalance(OfflinePlayer)");

            List<PlayerRow> rows = new ArrayList<>();
            for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
                String name = offline.getName();
                if (name == null || name.isBlank()) continue;
                double balance = ((Number) balanceMethod.invoke(economy, offline)).doubleValue();
                rows.add(new PlayerRow(offline, balance));
            }
            rows.sort(Comparator.comparingDouble(PlayerRow::balance).reversed()
                    .thenComparing(row -> Optional.ofNullable(row.player().getName()).orElse(""), String.CASE_INSENSITIVE_ORDER));

            BoardHolder holder = new BoardHolder(Type.BALTOP);
            Inventory inv = Bukkit.createInventory(holder, 27, TextUtil.component("&6&lBalance Top 10"));
            holder.bind(inv);
            fill(inv);

            int[] slots = {4, 10, 11, 12, 13, 14, 15, 16, 20, 22};
            for (int i = 0; i < Math.min(10, rows.size()); i++) {
                PlayerRow row = rows.get(i);
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                meta.setOwningPlayer(row.player());
                String name = Optional.ofNullable(row.player().getName()).orElse("Unknown");
                meta.displayName(TextUtil.component("&6#" + (i + 1) + " &f" + name));
                meta.lore(List.of(
                        TextUtil.component("&7Player: &f" + name),
                        TextUtil.component("&7Balance: &a$" + money(row.balance()))
                ));
                head.setItemMeta(meta);
                inv.setItem(slots[i], head);
            }
            player.openInventory(inv);
            return true;
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().warning("Could not build BalTop NPC GUI: " + ex.getMessage());
            plugin.msg(player, "&cCould not load BalTop right now.");
            return false;
        }
    }

    public void click(Player player, BoardHolder holder, ItemStack clicked) {
        if (holder.type() != Type.FTOP || clicked == null || !clicked.hasItemMeta()) return;
        String faction = clicked.getItemMeta().getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "ftop_faction"),
                org.bukkit.persistence.PersistentDataType.STRING);
        if (faction == null || faction.isBlank()) return;
        player.closeInventory();
        Bukkit.dispatchCommand(player, "f info " + faction);
    }

    private Object vaultEconomy() {
        try {
            Class<?> type = Class.forName("net.milkbowl.vault.economy.Economy");
            return Bukkit.getServicesManager().load(type);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    private void fill(Inventory inv) {
        ItemStack pane = button(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);
    }

    private ItemStack button(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextUtil.component(name));
        meta.lore(lore.stream().map(TextUtil::component).toList());
        item.setItemMeta(meta);
        return item;
    }

    private static String money(double value) {
        return String.format(Locale.US, "%,.2f", value);
    }

    private record FactionRow(String name, double land, double bank, int members, int claims, double power) {
        double total() { return land + bank; }
    }
    private record PlayerRow(OfflinePlayer player, double balance) {}

    public static final class BoardHolder implements InventoryHolder {
        private final Type type;
        private Inventory inventory;
        public BoardHolder(Type type) { this.type = type; }
        public Type type() { return type; }
        public void bind(Inventory inventory) { this.inventory = inventory; }
        @Override public @NotNull Inventory getInventory() { return inventory; }
    }
}
