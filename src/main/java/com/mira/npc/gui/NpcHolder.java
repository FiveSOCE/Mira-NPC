package com.mira.npc.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class NpcHolder implements InventoryHolder {
    public enum Type { MAIN, MANAGE, EDITOR }

    private final Type type;
    private final String npcId;
    private Inventory inventory;

    public NpcHolder(Type type, String npcId) {
        this.type = type;
        this.npcId = npcId;
    }

    public Type type() { return type; }
    public String npcId() { return npcId; }
    public void bind(Inventory inventory) { this.inventory = inventory; }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
