# MiraNPC

MiraNPC provides persistent VILLAGER, HOLOGRAM and native Paper Mannequin-backed PLAYER NPCs for the Mira Paper server suite. NPCs can act as interactive command triggers, live PlaceholderAPI displays, rotating leaderboards and timed-state displays.

## Download

[**Download MiraNPC v0.1.3**](https://github.com/FiveSOCE/Mira-NPC/releases/download/v0.1.3/MiraNPC-0.1.3.jar)

[View All Releases](https://github.com/FiveSOCE/Mira-NPC/releases)

## Requirements / Dependencies

- Paper 1.21.11
- Java 21
- PlaceholderAPI optional for dynamic NPC display names
- MiraFactions optional for faction/FTop placeholders used through PlaceholderAPI

## How MiraNPC Works

Administrators create NPC definitions through the `/mnpc` editor GUI. A definition contains the NPC display name, the command/function it executes and whether that function runs as the clicking `PLAYER` or as `CONSOLE`. Definitions persist in `plugins/MiraNPC/npcs.yml`; placed instances persist in `plugins/MiraNPC/placed.yml`.

VILLAGER mode uses the existing fixed native Villager body. HOLOGRAM mode hides the internal anchor and uses modern TextDisplay lines plus a dedicated Interaction hitbox, so players can click floating text without a visible mob body. PLAYER mode uses Minecraft/Paper's native Mannequin entity to create a player-avatar body with a configured Minecraft skin. No Citizens, ProtocolLib, PacketEvents or NMS dependency is required.

Floating text supports multiple PlaceholderAPI-resolved lines and configurable rotation frames. Timed states can change visibility, text and command chains for time windows that cross midnight safely. Command chains preserve PLAYER or CONSOLE execution per action. Function text supports `%player%`, `%username%`, `%uuid%` and player-context PlaceholderAPI values.

## Commands

All MiraNPC administration commands require `miranpc.admin`.

| Command | Permission | What it does |
| --- | --- | --- |
| `/mnpc` | `miranpc.admin` | Opens the MiraNPC creator/editor GUI. |
| `/mnpc place <NPC>` | `miranpc.admin` | Places the selected NPC on top of the block the administrator is looking at. |
| `/mnpc remove` | `miranpc.admin` | Removes the placed MiraNPC the administrator is looking directly at. |
| `/mnpc delete <NPC>` | `miranpc.admin` | Deletes an NPC definition and associated managed state. |
| `/mnpc mode <npc> <villager|hologram|player>` | `miranpc.admin` | Changes the NPC body/display backend. |
| `/mnpc skin <npc> <minecraftName|clear>` | `miranpc.admin` | Sets/clears the native Mannequin player skin and refreshes it live. |
| `/mnpc lines <npc> <line1|line2|...>` | `miranpc.admin` | Configures multi-line floating text. |
| `/mnpc action add <npc> <player|console> <command>` | `miranpc.admin` | Appends an action to the NPC command chain. |
| `/mnpc action clear <npc>` | `miranpc.admin` | Clears the command chain and falls back to the base command. |
| `/mnpc rotation <npc> <seconds|clear>` | `miranpc.admin` | Controls rotating display frames. |
| `/mnpc frame <npc> <number> <line1|line2|...>` | `miranpc.admin` | Creates/updates a rotating text frame. |
| `/mnpc state set <npc> <state> <HH:mm> <HH:mm> <visible> <lines>` | `miranpc.admin` | Creates a timed display state. |
| `/mnpc state remove <npc> <state>` | `miranpc.admin` | Removes a timed state. |
| `/mnpc status <npc>` | `miranpc.admin` | Shows mode, skin, frame/action/state counts and the native PLAYER backend status. |
| `/mnpc reload` | `miranpc.admin` | Reloads base definitions, extension state and display state. |

Regular players do not need a MiraNPC permission merely to click an NPC. The command executed by the NPC can still enforce its own permissions.

## Permissions

| Permission | Default | What it does |
| --- | --- | --- |
| `miranpc.admin` | OP | Allows all NPC creation, editing, placement, removal and reload tools. |

## Native PLAYER NPCs (0.1.3)

Citizens has been completely removed from MiraNPC.

On Paper 1.21.11, PLAYER mode now uses the native `org.bukkit.entity.Mannequin` API introduced by modern Minecraft/Paper. The Mannequin is a real player-avatar entity rather than a packet-only fake player.

MiraNPC keeps its existing invisible Villager anchor as the persistent instance/location authority, then manages the native Mannequin body for PLAYER mode. This preserves the existing placement, persistence, hologram, timed-state and action systems.

PLAYER mode currently provides:

- player-shaped native Minecraft avatar body
- Minecraft username skin resolution using Paper's asynchronous profile API
- cached resolved profiles to avoid repeating network lookups every display refresh
- immovable, invulnerable and non-collidable NPC bodies
- MiraNPC PDC identity on the native body so existing click/damage interaction handling still works
- floating text, PlaceholderAPI, command chains, rotations and timed states exactly as before
- automatic cleanup of old/non-native PLAYER body entities when refreshed

This removes the paid Citizens dependency and keeps MiraNPC on the public Paper API rather than version-fragile NMS packets.
