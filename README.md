# MiraNPC

MiraNPC provides persistent, immobile Villager NPCs for the Mira Paper server suite. Each NPC acts as a physical trigger that can run a configured player or console command, with optional PlaceholderAPI-powered dynamic display names.

## Download

[**Download MiraNPC v0.1.1**](https://github.com/FiveSOCE/Mira-NPC/releases/download/v0.1.1/MiraNPC-0.1.1.jar)

## Requirements / Dependencies

- Paper 1.21.11
- Java 21
- PlaceholderAPI optional for dynamic NPC display names
- MiraFactions optional for faction/FTop placeholders used through PlaceholderAPI

## How MiraNPC Works

Administrators create NPC definitions through the `/mnpc` editor GUI. A definition contains the NPC display name, the command/function it executes and whether that function runs as the clicking `PLAYER` or as `CONSOLE`. Definitions persist in `plugins/MiraNPC/npcs.yml`; placed instances persist in `plugins/MiraNPC/placed.yml`.

Placed NPCs are persistent Villagers with AI disabled. They do not wander or despawn, cannot be damaged, burned, leashed or transformed, and a position watchdog keeps them fixed to their saved location. Both left-click and right-click execute the configured function. Definition changes automatically propagate to placed instances.

Display names can contain PlaceholderAPI values and are refreshed on a configurable interval, making NPCs suitable for live displays such as MiraFactions FTop rankings. Function text can use `%player%`, `%username%` and `%uuid%`. For example, an NPC configured with function `ah` and executor `PLAYER` behaves like the clicking player ran `/ah` themselves, including any permissions that command normally requires.

## Commands

All MiraNPC administration commands require `miranpc.admin`.

| Command | Permission | What it does |
| --- | --- | --- |
| `/mnpc` | `miranpc.admin` | Opens the MiraNPC creator/editor GUI. |
| `/mnpc place <NPC>` | `miranpc.admin` | Places the selected NPC on top of the block the administrator is looking at. |
| `/mnpc remove` | `miranpc.admin` | Removes the placed MiraNPC the administrator is looking directly at. |
| `/mnpc delete <NPC>` | `miranpc.admin` | Deletes an NPC definition and associated managed state. |
| `/mnpc reload` | `miranpc.admin` | Reloads MiraNPC definitions/configuration. |

Regular players do not need a MiraNPC permission merely to click an NPC. The command executed by the NPC can still enforce its own permissions.

## Permissions

| Permission | Default | What it does |
| --- | --- | --- |
| `miranpc.admin` | OP | Allows all NPC creation, editing, placement, removal and reload tools. |
