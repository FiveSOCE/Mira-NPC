# Mira-NPC

Persistent villager NPCs that act as physical command/function triggers.

## Download

[Download MiraNPC v0.1.0 (.jar)](https://github.com/FiveSOCE/Mira-NPC/releases/download/v0.1.0/MiraNPC-0.1.0.jar)

[View all releases](https://github.com/FiveSOCE/Mira-NPC/releases)

## Features

- `/mnpc` opens the in-game creator/editor GUI.
- Create NPC definitions entirely in-game.
- Configure an NPC display name.
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
