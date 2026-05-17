# mineUGC Phase 1: Core Framework + Custom Items

## Overview

mineUGC is a Minecraft (Paper 1.21) plugin platform for User Generated Content.
Phase 1 establishes the foundational architecture and custom item system,
enabling players to create, edit, and use custom items with attributes,
abilities, and crafting recipes — all via in-game GUI and YAML configuration.

## Architecture

### Gradle Multi-Module Project

```
mineUGC/
├── build.gradle          (root — aggregates submodules)
├── settings.gradle
├── core/                 — Interfaces, registry, events, command framework
├── storage/              — YAML loader/watcher + SQLite runtime state
├── items/                — Item system (attributes, abilities, sets, crafting)
├── gui/                  — In-game editing GUI
└── plugin/               — Entry point with plugin.yml
```

### Dependency Graph

```
plugin → core ← storage → SQLite
plugin → items → core
plugin → gui → items, core
```

`core` has zero dependencies on other modules — it is the system contract.

## Data Model

### Item Definition (YAML)

Each item is one `.yml` file in `plugins/mineUGC/items/`:

```yaml
id: demo_sword
name: "&6Demo Sword"
material: DIAMOND_SWORD
model: 10001
lore:
  - "A test sword"
attributes:
  damage: 15
  speed: 1.6
abilities:
  right_click:
    type: lightning_strike
    cooldown: 10
    mana_cost: 20
passives:
  on_wear:
    type: potion_effect
    effect: speed
    amplifier: 1
set: demo_set
recipe:
  shape: ["DDD", "D D", " S"]
  ingredients:
    D: DIAMOND
    S: STICK
```

### Core Java Types

- `UgcAsset` — interface for all UGC content types
- `ItemDefinition` — immutable POJO parsed from YAML
- `AssetRegistry` — registry for lookup/replace/reload
- `AttributeApplier` — maps definition attributes to NBT/vanilla attributes
- `AbilityExecutor` — dispatches predefined ability types
- `PassiveEffect` — applies continuous effects per equip state
- `SetBonusTracker` — detects armor sets and applies bonuses
- `CustomRecipeManager` — registers/unregisters crafting recipes

## Hot Reload

### Flow

1. `Java FileWatcher` detects `.yml` change in `items/` directory
2. `YamlWatcher` parses file → `ItemDefinition`
3. Validation: material exists, numeric ranges, required fields
4. `AssetRegistry.replace(id, newDefinition)` — atomic swap
5. Fires `AssetReloadEvent` (Bukkit event)
6. `ItemManager` updates cache
7. Active push: scans online players' inventories, replaces matching items

### Update Strategy

- **Active push** on reload: scan online player inventories for items matching the changed ID, replace in-place
- **Lazy fallback** for offline players: items update on first interaction after login

## GUI Editor

### Commands

```
/ugc item list          — list all items
/ugc item give <id>     — give item to self
/ugc item reload        — manual hot reload
/ugc edit               — open GUI editor
/ugc reload             — reload all assets
```

### Editor Flow

Main menu → New/Edit → Property submenus → Save → Writes YAML → Triggers hot reload

Property editors: name, material, model ID, lore, attributes, abilities, passives, set, recipe. Each opens a dedicated submenu (virtual anvil input for text, item selector for materials, slider for numbers).

## Ability System

Abilities are **configuration-driven, not scripted**. Predefined effect types with parameters:

- `lightning_strike` — strike lightning at target
- `potion_effect` — apply potion effect (self or target)
- `projectile` — fire a custom projectile
- `explosion` — create explosion
- `heal` — heal self or target
- `teleport` — teleport to target location
- `summon` — summon entity
- `particle_ring` — display particle ring

Future Phase 3 (Scripting Engine) will allow `script: my_script.js` as an ability type.

## Storage

### YAML (definitions)
- Items directory: `plugins/mineUGC/items/*.yml`
- File watcher for hot reload
- Definitions never written to SQLite

### SQLite (runtime state only)
- `player_cooldowns` — UUID, item_id, ability_key, expires_at
- `player_data` — UUID, unlocked_items (JSON)

## Data Flow

**Write path:** GUI edit → YamlWriter → `items/*.yml`
**Read path:** YamlWatcher → ItemDefinition → AssetRegistry → ItemManager → in-game
**Runtime:** Cooldowns/state → SQLite

## Error Handling

| Scenario | Behavior |
|---|---|
| Invalid YAML | Reject file, log error, continue loading others |
| Duplicate item ID | Last-write-wins with warning |
| Ability execution error | Catch, inform player, continue tick |
| GUI invalid input | Reject with feedback, no write |
| SQLite failure | Degrade to in-memory cache |

## Testing

- **Unit tests** (JUnit 5): YAML parsing, attribute calculation, set bonus detection, registry operations
- **Integration tests** (Paper test env): item give, ability trigger, hot reload end-to-end
- **Not tested:** GUI interaction (manual QA)

## Out of Scope (Phase 1)

- Custom blocks (Phase 2)
- Scripting engine (Phase 3)
- NPC/quest system (Phase 4)
- Cosmetics (Phase 5)
- Minigame framework (Phase 6)
- Marketplace (Phase 7)
