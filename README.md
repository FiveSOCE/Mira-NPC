# MiraNPC

MiraNPC provides persistent VILLAGER, HOLOGRAM and optional Citizens-backed PLAYER NPCs for the Mira Paper server suite. NPCs can act as interactive command triggers, live PlaceholderAPI displays, rotating leaderboards and timed-state displays.

## Download

[**Download MiraNPC v0.1.2**](https://github.com/FiveSOCE/Mira-NPC/releases/download/v0.1.2/MiraNPC-0.1.2.jar)

## Requirements / Dependencies

- Paper 1.21.11
- Java 21
- PlaceholderAPI optional for dynamic NPC display names
- MiraFactions optional for faction/FTop placeholders used through PlaceholderAPI
- Citizens optional and required only for true PLAYER NPC bodies/skins

## How MiraNPC Works

Administrators create NPC definitions through the `/mnpc` editor GUI. A definition contains the NPC display name, the command/function it executes and whether that function runs as the clicking `PLAYER` or as `CONSOLE`. Definitions persist in `plugins/MiraNPC/npcs.yml`; placed instances persist in `plugins/MiraNPC/placed.yml`.

VILLAGER mode uses the existing fixed native Villager body. HOLOGRAM mode hides the internal anchor and uses modern TextDisplay lines plus a dedicated Interaction hitbox, so players can click floating text without a visible mob body. PLAYER mode uses an optional Citizens backend to create a real player-shaped NPC and apply a configured Minecraft skin; if Citizens is absent, MiraNPC remains fully functional in VILLAGER and HOLOGRAM modes.

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
| `/mnpc skin <npc> <minecraftName|clear>` | `miranpc.admin` | Sets/clears the Citizens player skin and refreshes it live. |
| `/mnpc lines <npc> <line1|line2|...>` | `miranpc.admin` | Configures multi-line floating text. |
| `/mnpc action add <npc> <player|console> <command>` | `miranpc.admin` | Appends an action to the NPC command chain. |
| `/mnpc action clear <npc>` | `miranpc.admin` | Clears the command chain and falls back to the base command. |
| `/mnpc rotation <npc> <seconds|clear>` | `miranpc.admin` | Controls rotating display frames. |
| `/mnpc frame <npc> <number> <line1|line2|...>` | `miranpc.admin` | Creates/updates a rotating text frame. |
| `/mnpc state set <npc> <state> <HH:mm> <HH:mm> <visible> <lines>` | `miranpc.admin` | Creates a timed display state. |
| `/mnpc state remove <npc> <state>` | `miranpc.admin` | Removes a timed state. |
| `/mnpc status <npc>` | `miranpc.admin` | Shows mode, skin, frame/action/state counts and Citizens backend status. |
| `/mnpc reload` | `miranpc.admin` | Reloads base definitions, extension state and display state. |

Regular players do not need a MiraNPC permission merely to click an NPC. The command executed by the NPC can still enforce its own permissions.

## Permissions

| Permission | Default | What it does |
| --- | --- | --- |
| `miranpc.admin` | OP | Allows all NPC creation, editing, placement, removal and reload tools. |
