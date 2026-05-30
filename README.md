# HandyDandy

Quality-of-life Fabric mod for Minecraft 26.1.2 — custom commands, crafting recipes, item abilities, and passive server features.

## Requirements

- Minecraft 26.1.2
- Fabric Loader 0.19.2+
- Fabric API

**Server-side only** — players do not need to install the mod on their client.

## Installation

Drop the jar into your server's `mods/` folder and restart.

## Features

### Commands

| Command | Description |
|---------|-------------|
| `/heal` | Restore health for an XP cost |
| `/setHome [name]` | Save your current location (default name: `home`) |
| `/tpHome [name]` | Teleport to a saved location |
| `/delHome <name>` | Delete a saved location |
| `/tpList` | List all your saved locations |

All commands also accept lowercase spellings: `/sethome`, `/tphome`, `/delhome`, `/tplist`.

Teleport commands require wearing a helmet. Up to 5 saved locations per player (configurable).

### Item Abilities

| Item | Ability |
|------|---------|
| Iron or Diamond Sword (right-click) | Night vision while held; clears when you switch away |
| Iron or Diamond Axe + Sneak (right-click) | Launch a shulker bullet |
| Swim Fins (crafted) | Depth Strider VII + Respiration III |
| Jump Boots (crafted) | Feather Falling IV + Protection IV, immunity to fall damage |

### Passive Features

- **Tree felling** — Break any log with an axe to fell the whole tree including leaves
- **Death waypoint** — Automatically saves a `death` location when you die; use `/tpHome death` to return
- **Pet teleport** — `/tpHome` brings nearby owned tame animals along with you
- **Join announcements** — Welcome message on join plus arrival broadcast to other online players

### Custom Recipes

| Result | Ingredients |
|--------|-------------|
| Diamond | Gunpowder, gravel, sand, iron ingot |
| Saddle | Leather, string, iron ingot |
| Name Tag | String, paper |
| Copper Pickaxe | Copper ingots, diamond, iron bars |
| Swim Fins | Lily pad × 3, leather boots, emerald |
| Jump Boots | Leather × 3, leather boots, emerald |
| Cow Spawn Egg | Diamond × 4, beef × 2, leather × 2, egg |
| Pig Spawn Egg | Diamond × 4, porkchop × 2, cooked porkchop × 2, egg |
| Spider Spawn Egg | Diamond × 4, spider eye × 2, string × 2, egg |
| Ginsu Iron Sword | Smelt an iron sword → Sharpness V |
| Ouchy Diamond Sword | Smelt a diamond sword → Sharpness X |

## Configuration

A config file is created at `config/handydandy.json` on first launch:

```json
{
  "enableHeal": true,
  "healXpType": "variable",
  "healXpCost": 0.5,
  "enableTp": true,
  "maxLocations": 5
}
```

`healXpType` can be `"variable"` (cost scales with missing health) or `"fixed"` (flat cost set by `healXpCost`).

## License

MIT
