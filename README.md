# Mira-NPC

Persistent villager NPCs that act as physical command/function triggers.

## Download

[Download MiraNPC v0.1.1 (.jar)](https://github.com/FiveSOCE/Mira-NPC/releases/download/v0.1.1/MiraNPC-0.1.1.jar)

[View all releases](https://github.com/FiveSOCE/Mira-NPC/releases)

## v0.1.1 dynamic display names

NPC display names can now contain PlaceholderAPI placeholders. MiraNPC refreshes dynamic names every 100 ticks by default, configurable with:

```yaml
settings:
  dynamic-name-refresh-ticks: 100
```

This allows live MiraFactions FTop displays without hard-coded faction data. Example NPC display name:

```text
&6#1 &f%mirafactions_top_1_name% &7- &a$%mirafactions_top_1_value%
```

Because MiraFactions exposes static FTop placeholders for ranks 1 through 10, NPCs can display the current top faction, wealth, land value, bank, power or member count and update automatically.

PlaceholderAPI and MiraFactions are optional soft dependencies. Normal static NPC names continue working without them.

## Features

- `/mnpc` opens the in-game creator/editor GUI.
- Create NPC definitions entirely in-game.
- Configure an NPC display name.
- Dynamic PlaceholderAPI-backed display names.
- Configure the command/function it runs.
- Toggle whether the function runs as the clicking PLAYER or as CONSOLE.
- `/mnpc place <NPC>` places the NPC on top of the block the admin is looking at.
- NPCs use persistent Villagers.
- NPCs have no AI, do not wander, do not despawn, cannot be damaged, cannot burn, cannot be leashed, cannot transform, and cannot teleport away.
- A position watchdog keeps each NPC fixed to its saved spawn position.
- Left-click and right-click both execute the configured function.
- Placed NPCs automatically inherit name/function changes made to their definition.
- Definitions persist in `plugins/MiraNPC/npcs.yml`.
- Placed instances persist in `plugins/MiraNPC/placed.yml`.

## Commands

```text
/mnpc
/mnpc place <NPC>
/mnpc remove
/mnpc delete <NPC>
/mnpc reload
```

`/mnpc remove` removes the placed MiraNPC the admin is looking directly at.

## Permission

```text
miranpc.admin
```

All creator/editor/placement/removal commands and GUIs are admin-only.

Regular players require no MiraNPC permission to interact with an NPC. The configured command itself may still enforce its own permissions.

## Function placeholders

```text
%player%
%username%
%uuid%
```

Example NPC function:

```text
ah
```

With executor `PLAYER`, clicking the NPC is equivalent to that player running `/ah` themselves.
