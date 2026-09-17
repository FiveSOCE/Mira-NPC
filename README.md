# MiraNPC

Persistent interactive NPC and hologram system for the Mira Paper server suite.

MiraNPC supports native Paper player-avatar NPCs, Villager NPCs and hologram-only interactions without Citizens, ProtocolLib, PacketEvents or version-fragile NMS packets.

## Current Release

**v0.1.8** — compatible with Paper/Minecraft **1.21.11 through 26.2** using Java 21 bytecode.

[View releases](https://github.com/FiveSOCE/Mira-NPC/releases)

## Requirements / Integrations

- Paper 1.21.11 through 26.2
- Java 21
- PlaceholderAPI optional for dynamic text
- MiraFactions optional for faction/FTop data consumed through integrations/placeholders
- Vault-compatible economy provider used by economy leaderboard functionality where applicable

## NPC Modes

### VILLAGER

Uses a native Villager body as an interactive NPC.

### HOLOGRAM

Uses modern TextDisplay lines with a dedicated Interaction hitbox. The internal anchor remains hidden so the player sees floating interactive text rather than a mob body.

### PLAYER

Uses Paper's native `Mannequin` entity to create a real player-avatar body with Minecraft profile/skin support.

PLAYER mode includes:

- asynchronous Minecraft profile resolution
- cached resolved profiles
- immovable/invulnerable/non-collidable presentation
- persistent MiraNPC identity
- normal MiraNPC click/damage handling
- the same floating text, actions, rotations and timed states as other modes

## Persistence

Definitions and placed instances are stored separately:

```text
plugins/MiraNPC/npcs.yml
plugins/MiraNPC/placed.yml
```

Definitions contain display/action configuration. Placed-instance data owns persistent locations and runtime restoration.

## Text and Actions

MiraNPC supports:

- multiple floating text lines
- PlaceholderAPI resolution
- rotating display frames
- timed display states
- visibility changes by time window
- chained PLAYER or CONSOLE actions

Action text supports placeholders such as:

```text
%player%
%username%
%uuid%
```

plus player-context PlaceholderAPI values when PlaceholderAPI is installed.

## Built-in Leaderboard Functions

Special function values can open player-facing leaderboard GUIs directly:

### `ftop`

Also accepts `f top`.

Shows the top factions with information such as total value, land assets, bank, members, claims and power. Clicking a faction opens its faction information flow.

### `baltop`

Also accepts `bal top`.

Shows ranked player heads with current Vault-backed balances.

These player-facing GUIs do not require `miranpc.admin`.

## Placement and Editor Reliability — v0.1.8

Recent placement/editor hardening includes:

- Villager NPC placement searches for safe two-block space
- placement uses an intentional command spawn reason
- managed Villager bodies have gravity disabled
- blocked placements return clear administrator feedback
- NPC name/command chat input is captured privately and no longer leaks into public chat

## Commands

All administration commands require `miranpc.admin`.

| Command | Purpose |
| --- | --- |
| `/mnpc` | Opens the creator/editor GUI. |
| `/mnpc place <npc>` | Places the selected NPC. |
| `/mnpc remove` | Removes the placed MiraNPC being targeted. |
| `/mnpc delete <npc>` | Deletes an NPC definition and managed state. |
| `/mnpc mode <npc> <villager|hologram|player>` | Changes display/body mode. |
| `/mnpc skin <npc> <minecraftName|clear>` | Sets/clears the native player skin. |
| `/mnpc lines <npc> <line1|line2|...>` | Sets floating text. |
| `/mnpc action add <npc> <player|console> <command>` | Adds an action to the chain. |
| `/mnpc action clear <npc>` | Clears the action chain. |
| `/mnpc rotation <npc> <seconds|clear>` | Controls rotating frames. |
| `/mnpc frame <npc> <number> <lines>` | Creates/updates a frame. |
| `/mnpc state set <npc> ...` | Creates a timed state. |
| `/mnpc state remove <npc> <state>` | Removes a timed state. |
| `/mnpc status <npc>` | Shows runtime definition/body status. |
| `/mnpc reload` | Reloads definitions and display state. |

Regular players do not need a MiraNPC permission merely to click an NPC. The action executed by the NPC can enforce its own permissions.

## Permissions

| Permission | Default | Purpose |
| --- | --- | --- |
| `miranpc.admin` | OP | NPC creation, editing, placement, removal and reload tools. |

## Building

```bash
gradle clean build
```

The output JAR is created in `build/libs/`.
