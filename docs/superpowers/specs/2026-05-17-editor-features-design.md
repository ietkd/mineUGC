# GUI Editor — Remaining Features Design

Date: 2026-05-17

## Overview

Implement the 5 placeholder ("coming soon") buttons in the in-game item editor:
Attributes, Abilities, Passives, Set Bonus, and Recipe.

## Interaction Model

Mixed approach: simple features via chat prompt, complex features via sub-inventory.

| Feature | Type | UX Pattern |
|---------|------|------------|
| Set Bonus | Single string | Chat prompt (inline) |
| Attributes | Map<String, Double> | Sub-inventory list + chat add |
| Abilities | Map<String, AbilityConfig> | 2-level sub-inventory + chat |
| Passives | Map<String, PassiveConfig> | 2-level sub-inventory + chat |
| Recipe | RecipeConfig (shape + ingredients) | Sub-inventory + chat |

## Detailed Design

### 1. Set Bonus
- Click "套装" button → chat prompt: "输入套装名称"
- Display current value on button lore
- Handled by GuiListener `case "set"`

### 2. Attributes
- `AttributesInventory` (FastInv, 54 slots)
- Each attribute = item (key as name, value as lore), click to remove
- Slot 53: "Add" button → chat prompt "属性名 数值"
- Slot 49: "Back" → main editor
- Chat handler `case "attribute_add"` parses "name value" and updates def.getAttributes()

### 3. Abilities
- `AbilityListInventory` (54 slots): shows ability keys
  - Click key → `AbilityEditInventory`
  - "Add" → chat: "输入技能key"
  - Chat handler `case "ability_add"` creates new AbilityConfig
- `AbilityEditInventory` (36 slots): edit single ability
  - Type → chat prompt
  - Cooldown → chat prompt (numeric)
  - Mana Cost → chat prompt (numeric)
  - Delete button → remove and return to list
  - Back → list

### 4. Passives
- Same 2-level structure as Abilities
- `PassiveListInventory` → `PassiveEditInventory`
- Fields: Type, Effect, Amplifier

### 5. Recipe
- `RecipeInventory` (54 slots)
- Shows 3×3 shape grid using items (slot 0-8)
- Shows ingredient mappings as items with lore
- "Edit Shape" → chat: "ABA,AAA,CDC" (3 comma-separated strings)
- "Edit Ingredients" → chat: "A=DIAMOND,B=STICK"
- Back → main editor

## Files to Create
1. `gui/.../editor/AttributesInventory.java`
2. `gui/.../editor/AbilityListInventory.java`
3. `gui/.../editor/AbilityEditInventory.java`
4. `gui/.../editor/PassiveListInventory.java`
5. `gui/.../editor/PassiveEditInventory.java`
6. `gui/.../editor/RecipeInventory.java`

## Files to Modify
1. `gui/.../editor/ItemEditorInventory.java` — wire up all 5 buttons
2. `gui/.../editor/GuiListener.java` — add chat handlers
3. `core/.../resources/messages.yml` — add new message keys

## Edge Cases
- Empty maps (no attributes/abilities/passives): show "empty" message
- Invalid chat input (non-numeric cooldown): show error, keep session open
- Duplicate ability/passive key: warn and reject
- Missing recipe ingredients: recipe.isValid() check
