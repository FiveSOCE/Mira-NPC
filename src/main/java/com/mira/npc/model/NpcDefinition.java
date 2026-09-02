package com.mira.npc.model;

public record NpcDefinition(
        String id,
        String displayName,
        String command,
        Executor executor
) {
    public enum Executor {
        PLAYER,
        CONSOLE
    }
}
