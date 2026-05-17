# Editor Remaining Features Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the 5 "coming soon" editor features: Set Bonus, Attributes, Abilities, Passives, Recipe.

**Architecture:** Each feature uses a sub-inventory (FastInv) for listing/editing, with chat prompts for data entry. The EditSession tracks which sub-inventory to reopen after chat input. The GuiListener gets new `case` branches in its chat switch. Complex fields use compound field names (e.g., `ability_type`) with the session storing context (e.g., `editingAbilityKey`).

**Tech Stack:** Paper 1.21.1, FastInv, Bukkit Inventory API

---

### Task 1: Update EditSession for sub-inventory support

**Files:**
- Modify: `gui/src/main/java/org/mineUGC/gui/editor/EditSession.java`

- [ ] **Add reopen action and editing key fields**

```java
package org.mineUGC.gui.editor;

import org.bukkit.entity.Player;
import org.mineUGC.core.model.ItemDefinition;

import java.util.function.BiConsumer;

class EditSession {
    private final ItemDefinition definition;
    private final boolean existing;
    private final String originalId;
    private String pendingField;
    private String editingAbilityKey;
    private String editingPassiveKey;
    private BiConsumer<Player, GuiListener> reopenAction;

    EditSession(ItemDefinition definition, boolean existing) {
        this.definition = definition;
        this.existing = existing;
        this.originalId = definition.getId();
    }

    ItemDefinition getDefinition() { return definition; }
    boolean isExisting() { return existing; }
    String getOriginalId() { return originalId; }
    String getPendingField() { return pendingField; }
    void setPendingField(String pendingField) { this.pendingField = pendingField; }

    String getEditingAbilityKey() { return editingAbilityKey; }
    void setEditingAbilityKey(String key) { this.editingAbilityKey = key; }
    String getEditingPassiveKey() { return editingPassiveKey; }
    void setEditingPassiveKey(String key) { this.editingPassiveKey = key; }

    BiConsumer<Player, GuiListener> getReopenAction() { return reopenAction; }
    void setReopenAction(BiConsumer<Player, GuiListener> action) { this.reopenAction = action; }
}
```

- [ ] **Commit**

```bash
git add gui/src/main/java/org/mineUGC/gui/editor/EditSession.java
git commit -m "feat(editor): add sub-inventory support to EditSession"
```

---

### Task 2: Add all new messages to messages.yml

**Files:**
- Modify: `core/src/main/resources/messages.yml`

- [ ] **Append new message keys before the `items:` section**

After `invalid-number:` and before `items:`, add:

```yaml
  # Set Bonus
  field-set: "§e套装"
  prompt-set: "输入套装名称:"
  set-set: "§a套装已设为: §f%s"

  # Attributes
  prompt-attribute-add: "输入 属性名 数值 (例如 damage 5.0):"
  attribute-added: "§a已添加属性: §f%s §7= §f%s"
  attribute-removed: "§c已移除属性: %s"
  attr-none: "§7暂无属性"

  # Ability keys
  abilities-title: "§8技能列表 - %s"
  ability-edit-title: "§8编辑技能 - %s"
  prompt-ability-key: "输入技能key (例如 right_click):"
  ability-added: "§a已添加技能: %s"
  ability-deleted: "§c已删除技能: %s"
  field-type: "§e类型"
  field-cooldown: "§e冷却 (秒)"
  field-mana: "§e魔法消耗"
  field-delete: "§c§l删除"
  prompt-type: "输入技能类型 (例如 projectile):"
  prompt-cooldown: "输入冷却时间 (秒):"
  prompt-mana: "输入魔法消耗:"
  type-set: "§a类型已设为: §f%s"
  cooldown-set: "§a冷却已设为: %d秒"
  mana-set: "§a魔法消耗已设为: %d"
  ability-none: "§7暂无技能"

  # Passive keys
  passives-title: "§8被动列表 - %s"
  passive-edit-title: "§8编辑被动 - %s"
  prompt-passive-key: "输入被动key (例如 on_hit):"
  passive-added: "§a已添加被动: %s"
  passive-deleted: "§c已删除被动: %s"
  field-effect: "§e效果"
  field-amplifier: "§e放大倍数"
  prompt-effect: "输入效果类型 (例如 regeneration):"
  prompt-amplifier: "输入放大倍数:"
  effect-set: "§a效果已设为: §f%s"
  amplifier-set: "§a放大倍数已设为: %d"
  passive-none: "§7暂无被动"

  # Recipe
  recipe-title: "§8配方编辑 - %s"
  prompt-shape: "输入3行形状，用逗号分隔 (例如 ABA,AAA,CDC):"
  prompt-ingredients: "输入材料映射，逗号分隔 (例如 A=DIAMOND,B=STICK):"
  shape-set: "§a形状已设为"
  ingredients-set: "§a材料已设为"
  recipe-cleared: "§c配方已清除"
  edit-shape: "§a编辑形状"
  edit-ingredients: "§a编辑材料"
  clear-recipe: "§c清除配方"
```

- [ ] **Commit**

```bash
git add core/src/main/resources/messages.yml
git commit -m "feat(editor): add messages for remaining editor features"
```

---

### Task 3: Update GuiListener.reopenEditor to use reopen action

**Files:**
- Modify: `gui/src/main/java/org/mineUGC/gui/editor/GuiListener.java`

- [ ] **Replace the reopenEditor method**

Find this:
```java
    private void reopenEditor(Player player, EditSession session) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            new ItemEditorInventory(player, session.getDefinition(), itemManager, this).open(player);
        });
    }
```

Replace with:
```java
    private void reopenEditor(Player player, EditSession session) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            BiConsumer<Player, GuiListener> action = session.getReopenAction();
            if (action != null) {
                action.accept(player, this);
            } else {
                new ItemEditorInventory(player, session.getDefinition(), itemManager, this).open(player);
            }
        });
    }
```

Add import:
```java
import java.util.function.BiConsumer;
```

- [ ] **Commit**

```bash
git add gui/src/main/java/org/mineUGC/gui/editor/GuiListener.java
git commit -m "feat(editor): support custom reopen action in GuiListener"
```

---

### Task 4: Implement Set Bonus (inline chat prompt)

**Files:**
- Modify: `gui/src/main/java/org/mineUGC/gui/editor/GuiListener.java`
- Modify: `gui/src/main/java/org/mineUGC/gui/editor/ItemEditorInventory.java`

- [ ] **Add `case "set"` to GuiListener's chat switch**

After the `case "id"` block, add:
```java
            case "set" -> {
                def.setSet(input);
                player.sendMessage(messages.get("editor.set-set", input));
            }
```

- [ ] **Wire up the Set Bonus button in ItemEditorInventory**

Replace the coming-soon handler for slot 22:
```java
        setItem(22, createItem(Material.ENDER_EYE, m.get("editor.field-set-bonus"), current(def.getSet(), m)),
                e -> guiListener.promptField(player, "set", m.get("editor.prompt-set")));
```

- [ ] **Commit**

```bash
git add gui/src/main/java/org/mineUGC/gui/editor/GuiListener.java gui/src/main/java/org/mineUGC/gui/editor/ItemEditorInventory.java
git commit -m "feat(editor): implement set bonus editing"
```

---

### Task 5: Create AttributesInventory

**Files:**
- Create: `gui/src/main/java/org/mineUGC/gui/editor/AttributesInventory.java`

- [ ] **Write AttributesInventory**

```java
package org.mineUGC.gui.editor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineUGC.core.message.Messages;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.gui.fastinv.FastInv;
import org.mineUGC.items.ItemManager;

import java.util.HashMap;
import java.util.Map;

public class AttributesInventory extends FastInv {
    public AttributesInventory(Player player, ItemDefinition def, ItemManager itemManager, GuiListener guiListener) {
        super(54, "§8属性 - " + def.getId());

        Messages m = guiListener.getMessages();
        Map<String, Double> attrs = def.getAttributes();

        int slot = 0;
        if (attrs != null) {
            for (Map.Entry<String, Double> entry : attrs.entrySet()) {
                String attrName = entry.getKey();
                double value = entry.getValue();
                int s = slot;
                setItem(s, MainMenuInventory.createItem(Material.GLOWSTONE_DUST,
                        "§e" + attrName, "§7" + value, "§c点击移除"), click -> {
                    attrs.remove(attrName);
                    player.sendMessage(m.get("editor.attribute-removed", attrName));
                    new AttributesInventory(player, def, itemManager, guiListener).open(player);
                });
                slot++;
            }
        }
        if (slot == 0) {
            setItem(22, MainMenuInventory.createItem(Material.BARRIER,
                    m.get("editor.attr-none"), ""));
        }

        setItem(48, MainMenuInventory.createItem(Material.LIME_WOOL,
                "§a§l" + m.get("editor.click-configure"), "§7" + m.get("editor.prompt-attribute-add")),
                e -> {
                    guiListener.promptField(player, "attribute_add", m.get("editor.prompt-attribute-add"));
                    guiListener.getEditSession(player.getUniqueId()).ifPresent(session ->
                            session.setReopenAction((p, g) ->
                                    new AttributesInventory(p, def, itemManager, g).open(p)));
                });
        setItem(49, MainMenuInventory.createItem(Material.BARRIER, m.get("editor.field-back"), m.get("editor.back-lore")),
                e -> new ItemEditorInventory(player, def, itemManager, guiListener).open(player));
    }
}
```

- [ ] **Commit**

```bash
git add gui/src/main/java/org/mineUGC/gui/editor/AttributesInventory.java
git commit -m "feat(editor): create AttributesInventory sub-inventory"
```

---

### Task 6: Wire up Attributes in GuiListener and ItemEditorInventory

**Files:**
- Modify: `gui/src/main/java/org/mineUGC/gui/editor/GuiListener.java`
- Modify: `gui/src/main/java/org/mineUGC/gui/editor/ItemEditorInventory.java`

- [ ] **Add `case "attribute_add"` to GuiListener chat switch**

After the `case "set"` block, add:
```java
            case "attribute_add" -> {
                String[] parts = input.split(" ");
                if (parts.length == 2) {
                    try {
                        double val = Double.parseDouble(parts[1]);
                        if (def.getAttributes() == null) def.setAttributes(new HashMap<>());
                        def.getAttributes().put(parts[0], val);
                        player.sendMessage(messages.get("editor.attribute-added", parts[0], parts[1]));
                    } catch (NumberFormatException e) {
                        player.sendMessage(messages.get("editor.invalid-number"));
                        return;
                    }
                } else {
                    player.sendMessage(messages.get("editor.invalid-number"));
                    return;
                }
            }
```

Also update imports (`HashMap` should already be imported via `java.util.*`).

- [ ] **Replace Attributes button in ItemEditorInventory**

Find the slot 15 handler:
```java
        setItem(15, createItem(Material.DIAMOND_SWORD, m.get("editor.field-attributes"), m.get("editor.click-configure")),
                e -> player.sendMessage(m.get("editor.coming-soon", m.get("editor.field-attributes"))));
```

Replace with:
```java
        setItem(15, createItem(Material.DIAMOND_SWORD, m.get("editor.field-attributes"), m.get("editor.click-configure")),
                e -> new AttributesInventory(player, def, itemManager, guiListener).open(player));
```

- [ ] **Commit**

```bash
git add gui/src/main/java/org/mineUGC/gui/editor/GuiListener.java gui/src/main/java/org/mineUGC/gui/editor/ItemEditorInventory.java
git commit -m "feat(editor): wire up attribute editing"
```

---

### Task 7: Create AbilityListInventory and AbilityEditInventory

**Files:**
- Create: `gui/src/main/java/org/mineUGC/gui/editor/AbilityListInventory.java`
- Create: `gui/src/main/java/org/mineUGC/gui/editor/AbilityEditInventory.java`

- [ ] **Write AbilityListInventory**

```java
package org.mineUGC.gui.editor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.mineUGC.core.model.AbilityConfig;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.gui.fastinv.FastInv;
import org.mineUGC.items.ItemManager;

import java.util.Map;

public class AbilityListInventory extends FastInv {
    public AbilityListInventory(Player player, ItemDefinition def, ItemManager itemManager, GuiListener guiListener) {
        super(54, guiListener.getMessages().get("editor.abilities-title", def.getId()));

        Messages m = guiListener.getMessages();
        Map<String, AbilityConfig> abilities = def.getAbilities();

        int slot = 0;
        if (abilities != null) {
            for (Map.Entry<String, AbilityConfig> entry : abilities.entrySet()) {
                String key = entry.getKey();
                AbilityConfig ability = entry.getValue();
                int s = slot;
                setItem(s, MainMenuInventory.createItem(Material.BLAZE_POWDER,
                        "§e" + key,
                        "§7Type: " + (ability.getType() != null ? ability.getType() : "?"),
                        "§7Cooldown: " + ability.getCooldown() + "s",
                        "§7Mana: " + ability.getManaCost()), click -> {
                    guiListener.getEditSession(player.getUniqueId()).ifPresent(session ->
                            session.setEditingAbilityKey(key));
                    new AbilityEditInventory(player, def, key, itemManager, guiListener).open(player);
                });
                slot++;
            }
        }
        if (slot == 0) {
            setItem(22, MainMenuInventory.createItem(Material.BARRIER,
                    m.get("editor.ability-none"), ""));
        }

        setItem(48, MainMenuInventory.createItem(Material.LIME_WOOL,
                "§a§l" + m.get("editor.click-configure"), "§7" + m.get("editor.prompt-ability-key")),
                e -> {
                    guiListener.promptField(player, "ability_add", m.get("editor.prompt-ability-key"));
                    guiListener.getEditSession(player.getUniqueId()).ifPresent(session ->
                            session.setReopenAction((p, g) ->
                                    new AbilityListInventory(p, def, itemManager, g).open(p)));
                });
        setItem(49, MainMenuInventory.createItem(Material.BARRIER, m.get("editor.field-back"), m.get("editor.back-lore")),
                e -> new ItemEditorInventory(player, def, itemManager, guiListener).open(player));
    }
}
```

- [ ] **Write AbilityEditInventory**

```java
package org.mineUGC.gui.editor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.mineUGC.core.message.Messages;
import org.mineUGC.core.model.AbilityConfig;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.gui.fastinv.FastInv;
import org.mineUGC.items.ItemManager;

public class AbilityEditInventory extends FastInv {
    public AbilityEditInventory(Player player, ItemDefinition def, String abilityKey,
                                ItemManager itemManager, GuiListener guiListener) {
        super(36, guiListener.getMessages().get("editor.ability-edit-title", abilityKey));

        Messages m = guiListener.getMessages();
        AbilityConfig ability = def.getAbilities().get(abilityKey);

        setItem(4, MainMenuInventory.createItem(Material.BLAZE_POWDER, "§e" + abilityKey, ""));

        setItem(11, MainMenuInventory.createItem(Material.NAME_TAG, m.get("editor.field-type"),
                        m.get("editor.current", ability.getType() != null ? ability.getType() : "?")),
                e -> {
                    guiListener.promptField(player, "ability_type", m.get("editor.prompt-type"));
                    guiListener.getEditSession(player.getUniqueId()).ifPresent(session ->
                            session.setEditingAbilityKey(abilityKey));
                });
        setItem(13, MainMenuInventory.createItem(Material.CLOCK, m.get("editor.field-cooldown"),
                        m.get("editor.current", ability.getCooldown() + "s")),
                e -> {
                    guiListener.promptField(player, "ability_cooldown", m.get("editor.prompt-cooldown"));
                    guiListener.getEditSession(player.getUniqueId()).ifPresent(session ->
                            session.setEditingAbilityKey(abilityKey));
                });
        setItem(15, MainMenuInventory.createItem(Material.EXPERIENCE_BOTTLE, m.get("editor.field-mana"),
                        m.get("editor.current", String.valueOf(ability.getManaCost()))),
                e -> {
                    guiListener.promptField(player, "ability_mana", m.get("editor.prompt-mana"));
                    guiListener.getEditSession(player.getUniqueId()).ifPresent(session ->
                            session.setEditingAbilityKey(abilityKey));
                });

        setItem(31, MainMenuInventory.createItem(Material.RED_WOOL, m.get("editor.field-delete"),
                        "§7" + m.get("editor.ability-deleted", abilityKey)),
                e -> {
                    def.getAbilities().remove(abilityKey);
                    player.sendMessage(m.get("editor.ability-deleted", abilityKey));
                    new AbilityListInventory(player, def, itemManager, guiListener).open(player);
                });
        setItem(35, MainMenuInventory.createItem(Material.BARRIER, m.get("editor.field-back"),
                        m.get("editor.back-lore")),
                e -> new AbilityListInventory(player, def, itemManager, guiListener).open(player));
    }
}
```

- [ ] **Commit**

```bash
git add gui/src/main/java/org/mineUGC/gui/editor/AbilityListInventory.java gui/src/main/java/org/mineUGC/gui/editor/AbilityEditInventory.java
git commit -m "feat(editor): create ability sub-inventories"
```

---

### Task 8: Wire up Abilities in GuiListener and ItemEditorInventory

**Files:**
- Modify: `gui/src/main/java/org/mineUGC/gui/editor/GuiListener.java`
- Modify: `gui/src/main/java/org/mineUGC/gui/editor/ItemEditorInventory.java`

- [ ] **Add ability cases to GuiListener chat switch**

After the `case "attribute_add"` block, add:
```java
            case "ability_add" -> {
                if (def.getAbilities() == null) def.setAbilities(new HashMap<>());
                if (def.getAbilities().containsKey(input)) {
                    player.sendMessage("§c技能key已存在: " + input);
                    return;
                }
                def.getAbilities().put(input, new AbilityConfig());
                player.sendMessage(messages.get("editor.ability-added", input));
            }
            case "ability_type" -> {
                String key = session.getEditingAbilityKey();
                if (key != null && def.getAbilities() != null) {
                    AbilityConfig ability = def.getAbilities().get(key);
                    if (ability != null) {
                        ability.setType(input);
                        player.sendMessage(messages.get("editor.type-set", input));
                    }
                }
            }
            case "ability_cooldown" -> {
                try {
                    int val = Integer.parseInt(input);
                    String key = session.getEditingAbilityKey();
                    if (key != null && def.getAbilities() != null) {
                        AbilityConfig ability = def.getAbilities().get(key);
                        if (ability != null) {
                            ability.setCooldown(val);
                            player.sendMessage(messages.get("editor.cooldown-set", val));
                        }
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(messages.get("editor.invalid-number"));
                    return;
                }
            }
            case "ability_mana" -> {
                try {
                    int val = Integer.parseInt(input);
                    String key = session.getEditingAbilityKey();
                    if (key != null && def.getAbilities() != null) {
                        AbilityConfig ability = def.getAbilities().get(key);
                        if (ability != null) {
                            ability.setManaCost(val);
                            player.sendMessage(messages.get("editor.mana-set", val));
                        }
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(messages.get("editor.invalid-number"));
                    return;
                }
            }
```

Also verify imports: AbilityConfig should already be imported in GuiListener (need to add).

Add import:
```java
import org.mineUGC.core.model.AbilityConfig;
```

- [ ] **Replace Abilities button in ItemEditorInventory**

Find:
```java
        setItem(20, createItem(Material.BLAZE_POWDER, m.get("editor.field-abilities"), m.get("editor.click-configure")),
                e -> player.sendMessage(m.get("editor.coming-soon", m.get("editor.field-abilities"))));
```

Replace with:
```java
        setItem(20, createItem(Material.BLAZE_POWDER, m.get("editor.field-abilities"), m.get("editor.click-configure")),
                e -> new AbilityListInventory(player, def, itemManager, guiListener).open(player));
```

Add import for `HashMap` (or verify it's covered by `java.util.*` which should be imported in GuiListener).

- [ ] **Commit**

```bash
git add gui/src/main/java/org/mineUGC/gui/editor/GuiListener.java gui/src/main/java/org/mineUGC/gui/editor/ItemEditorInventory.java
git commit -m "feat(editor): wire up ability editing"
```

---

### Task 9: Create PassiveListInventory and PassiveEditInventory

**Files:**
- Create: `gui/src/main/java/org/mineUGC/gui/editor/PassiveListInventory.java`
- Create: `gui/src/main/java/org/mineUGC/gui/editor/PassiveEditInventory.java`

- [ ] **Write PassiveListInventory**

```java
package org.mineUGC.gui.editor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.mineUGC.core.message.Messages;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.core.model.PassiveConfig;
import org.mineUGC.gui.fastinv.FastInv;
import org.mineUGC.items.ItemManager;

import java.util.Map;

public class PassiveListInventory extends FastInv {
    public PassiveListInventory(Player player, ItemDefinition def, ItemManager itemManager, GuiListener guiListener) {
        super(54, guiListener.getMessages().get("editor.passives-title", def.getId()));

        Messages m = guiListener.getMessages();
        Map<String, PassiveConfig> passives = def.getPassives();

        int slot = 0;
        if (passives != null) {
            for (Map.Entry<String, PassiveConfig> entry : passives.entrySet()) {
                String key = entry.getKey();
                PassiveConfig passive = entry.getValue();
                int s = slot;
                setItem(s, MainMenuInventory.createItem(Material.POTION,
                        "§e" + key,
                        "§7Type: " + (passive.getType() != null ? passive.getType() : "?"),
                        "§7Effect: " + (passive.getEffect() != null ? passive.getEffect() : "?"),
                        "§7Amplifier: " + passive.getAmplifier()), click -> {
                    guiListener.getEditSession(player.getUniqueId()).ifPresent(session ->
                            session.setEditingPassiveKey(key));
                    new PassiveEditInventory(player, def, key, itemManager, guiListener).open(player);
                });
                slot++;
            }
        }
        if (slot == 0) {
            setItem(22, MainMenuInventory.createItem(Material.BARRIER,
                    m.get("editor.passive-none"), ""));
        }

        setItem(48, MainMenuInventory.createItem(Material.LIME_WOOL,
                "§a§l" + m.get("editor.click-configure"), "§7" + m.get("editor.prompt-passive-key")),
                e -> {
                    guiListener.promptField(player, "passive_add", m.get("editor.prompt-passive-key"));
                    guiListener.getEditSession(player.getUniqueId()).ifPresent(session ->
                            session.setReopenAction((p, g) ->
                                    new PassiveListInventory(p, def, itemManager, g).open(p)));
                });
        setItem(49, MainMenuInventory.createItem(Material.BARRIER, m.get("editor.field-back"), m.get("editor.back-lore")),
                e -> new ItemEditorInventory(player, def, itemManager, guiListener).open(player));
    }
}
```

- [ ] **Write PassiveEditInventory**

```java
package org.mineUGC.gui.editor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.mineUGC.core.message.Messages;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.core.model.PassiveConfig;
import org.mineUGC.gui.fastinv.FastInv;
import org.mineUGC.items.ItemManager;

public class PassiveEditInventory extends FastInv {
    public PassiveEditInventory(Player player, ItemDefinition def, String passiveKey,
                                ItemManager itemManager, GuiListener guiListener) {
        super(36, guiListener.getMessages().get("editor.passive-edit-title", passiveKey));

        Messages m = guiListener.getMessages();
        PassiveConfig passive = def.getPassives().get(passiveKey);

        setItem(4, MainMenuInventory.createItem(Material.POTION, "§e" + passiveKey, ""));

        setItem(11, MainMenuInventory.createItem(Material.NAME_TAG, m.get("editor.field-type"),
                        m.get("editor.current", passive.getType() != null ? passive.getType() : "?")),
                e -> {
                    guiListener.promptField(player, "passive_type", m.get("editor.prompt-type"));
                    guiListener.getEditSession(player.getUniqueId()).ifPresent(session ->
                            session.setEditingPassiveKey(passiveKey));
                });
        setItem(13, MainMenuInventory.createItem(Material.POTION, m.get("editor.field-effect"),
                        m.get("editor.current", passive.getEffect() != null ? passive.getEffect() : "?")),
                e -> {
                    guiListener.promptField(player, "passive_effect", m.get("editor.prompt-effect"));
                    guiListener.getEditSession(player.getUniqueId()).ifPresent(session ->
                            session.setEditingPassiveKey(passiveKey));
                });
        setItem(15, MainMenuInventory.createItem(Material.REPEATER, m.get("editor.field-amplifier"),
                        m.get("editor.current", String.valueOf(passive.getAmplifier()))),
                e -> {
                    guiListener.promptField(player, "passive_amplifier", m.get("editor.prompt-amplifier"));
                    guiListener.getEditSession(player.getUniqueId()).ifPresent(session ->
                            session.setEditingPassiveKey(passiveKey));
                });

        setItem(31, MainMenuInventory.createItem(Material.RED_WOOL, m.get("editor.field-delete"),
                        "§7" + m.get("editor.passive-deleted", passiveKey)),
                e -> {
                    def.getPassives().remove(passiveKey);
                    player.sendMessage(m.get("editor.passive-deleted", passiveKey));
                    new PassiveListInventory(player, def, itemManager, guiListener).open(player);
                });
        setItem(35, MainMenuInventory.createItem(Material.BARRIER, m.get("editor.field-back"),
                        m.get("editor.back-lore")),
                e -> new PassiveListInventory(player, def, itemManager, guiListener).open(player));
    }
}
```

- [ ] **Commit**

```bash
git add gui/src/main/java/org/mineUGC/gui/editor/PassiveListInventory.java gui/src/main/java/org/mineUGC/gui/editor/PassiveEditInventory.java
git commit -m "feat(editor): create passive sub-inventories"
```

---

### Task 10: Wire up Passives in GuiListener and ItemEditorInventory

**Files:**
- Modify: `gui/src/main/java/org/mineUGC/gui/editor/GuiListener.java`
- Modify: `gui/src/main/java/org/mineUGC/gui/editor/ItemEditorInventory.java`

- [ ] **Add passive cases to GuiListener chat switch**

After the `case "ability_mana"` block, add:
```java
            case "passive_add" -> {
                if (def.getPassives() == null) def.setPassives(new HashMap<>());
                if (def.getPassives().containsKey(input)) {
                    player.sendMessage("§c被动key已存在: " + input);
                    return;
                }
                def.getPassives().put(input, new PassiveConfig());
                player.sendMessage(messages.get("editor.passive-added", input));
            }
            case "passive_type" -> {
                String key = session.getEditingPassiveKey();
                if (key != null && def.getPassives() != null) {
                    PassiveConfig passive = def.getPassives().get(key);
                    if (passive != null) {
                        passive.setType(input);
                        player.sendMessage(messages.get("editor.type-set", input));
                    }
                }
            }
            case "passive_effect" -> {
                String key = session.getEditingPassiveKey();
                if (key != null && def.getPassives() != null) {
                    PassiveConfig passive = def.getPassives().get(key);
                    if (passive != null) {
                        passive.setEffect(input);
                        player.sendMessage(messages.get("editor.effect-set", input));
                    }
                }
            }
            case "passive_amplifier" -> {
                try {
                    int val = Integer.parseInt(input);
                    String key = session.getEditingPassiveKey();
                    if (key != null && def.getPassives() != null) {
                        PassiveConfig passive = def.getPassives().get(key);
                        if (passive != null) {
                            passive.setAmplifier(val);
                            player.sendMessage(messages.get("editor.amplifier-set", val));
                        }
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(messages.get("editor.invalid-number"));
                    return;
                }
            }
```

Add imports (should be covered by existing imports but verify `PassiveConfig`):
```java
import org.mineUGC.core.model.PassiveConfig;
```

- [ ] **Replace Passives button in ItemEditorInventory**

Find:
```java
        setItem(21, createItem(Material.POTION, m.get("editor.field-passives"), m.get("editor.click-configure")),
                e -> player.sendMessage(m.get("editor.coming-soon", m.get("editor.field-passives"))));
```

Replace with:
```java
        setItem(21, createItem(Material.POTION, m.get("editor.field-passives"), m.get("editor.click-configure")),
                e -> new PassiveListInventory(player, def, itemManager, guiListener).open(player));
```

- [ ] **Commit**

```bash
git add gui/src/main/java/org/mineUGC/gui/editor/GuiListener.java gui/src/main/java/org/mineUGC/gui/editor/ItemEditorInventory.java
git commit -m "feat(editor): wire up passive editing"
```

---

### Task 11: Create RecipeInventory

**Files:**
- Create: `gui/src/main/java/org/mineUGC/gui/editor/RecipeInventory.java`

- [ ] **Write RecipeInventory**

```java
package org.mineUGC.gui.editor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineUGC.core.message.Messages;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.core.model.RecipeConfig;
import org.mineUGC.gui.fastinv.FastInv;
import org.mineUGC.items.ItemManager;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RecipeInventory extends FastInv {
    public RecipeInventory(Player player, ItemDefinition def, ItemManager itemManager, GuiListener guiListener) {
        super(54, guiListener.getMessages().get("editor.recipe-title", def.getId()));

        Messages m = guiListener.getMessages();
        RecipeConfig recipe = def.getRecipe();

        // 3x3 shape grid (slots 0-8)
        if (recipe != null && recipe.getShape() != null) {
            List<String> shape = recipe.getShape();
            for (int row = 0; row < 3 && row < shape.size(); row++) {
                String line = shape.get(row);
                for (int col = 0; col < 3 && col < line.length(); col++) {
                    char c = line.charAt(col);
                    int slot = row * 9 + col;
                    Material mat = c == ' ' ? Material.AIR : Material.GRAY_STAINED_GLASS_PANE;
                    String name = c == ' ' ? "§8空" : "§e" + c;
                    String[] lore = {};
                    if (c != ' ' && recipe.getIngredients() != null && recipe.getIngredients().containsKey(String.valueOf(c))) {
                        lore = new String[]{"§7→ " + recipe.getIngredients().get(String.valueOf(c))};
                    }
                    setItem(slot, MainMenuInventory.createItem(mat, name, lore));
                }
            }
        } else {
            for (int i = 0; i < 9; i++) {
                setItem(i, MainMenuInventory.createItem(Material.AIR, ""));
            }
        }

        // Instructions
        setItem(20, MainMenuInventory.createItem(Material.OAK_SIGN, m.get("editor.field-shape"), "§7" + m.get("editor.prompt-shape")), null);
        setItem(24, MainMenuInventory.createItem(Material.MAP, m.get("editor.field-ingredients"), "§7" + m.get("editor.prompt-ingredients")), null);

        // Ingredient display
        if (recipe != null && recipe.getIngredients() != null) {
            int slot = 36;
            for (Map.Entry<String, String> ing : recipe.getIngredients().entrySet()) {
                if (slot >= 45) break;
                setItem(slot, MainMenuInventory.createItem(Material.NAME_TAG,
                        "§e" + ing.getKey(), "§7→ " + ing.getValue()));
                slot++;
            }
        }

        // Buttons
        setItem(30, MainMenuInventory.createItem(Material.LIME_WOOL, m.get("editor.edit-shape"), "§7" + m.get("editor.prompt-shape")),
                e -> {
                    guiListener.promptField(player, "recipe_shape", m.get("editor.prompt-shape"));
                    guiListener.getEditSession(player.getUniqueId()).ifPresent(session ->
                            session.setReopenAction((p, g) ->
                                    new RecipeInventory(p, def, itemManager, g).open(p)));
                });
        setItem(32, MainMenuInventory.createItem(Material.LIME_WOOL, m.get("editor.edit-ingredients"), "§7" + m.get("editor.prompt-ingredients")),
                e -> {
                    guiListener.promptField(player, "recipe_ingredients", m.get("editor.prompt-ingredients"));
                    guiListener.getEditSession(player.getUniqueId()).ifPresent(session ->
                            session.setReopenAction((p, g) ->
                                    new RecipeInventory(p, def, itemManager, g).open(p)));
                });
        setItem(34, MainMenuInventory.createItem(Material.RED_WOOL, m.get("editor.clear-recipe"), ""),
                e -> {
                    def.setRecipe(null);
                    player.sendMessage(m.get("editor.recipe-cleared"));
                    new RecipeInventory(player, def, itemManager, guiListener).open(player);
                });

        setItem(49, MainMenuInventory.createItem(Material.BARRIER, m.get("editor.field-back"), m.get("editor.back-lore")),
                e -> new ItemEditorInventory(player, def, itemManager, guiListener).open(player));
    }
}
```

- [ ] **Commit**

```bash
git add gui/src/main/java/org/mineUGC/gui/editor/RecipeInventory.java
git commit -m "feat(editor): create RecipeInventory"
```

---

### Task 12: Wire up Recipe in GuiListener and ItemEditorInventory

**Files:**
- Modify: `gui/src/main/java/org/mineUGC/gui/editor/GuiListener.java`
- Modify: `gui/src/main/java/org/mineUGC/gui/editor/ItemEditorInventory.java`

- [ ] **Add recipe cases to GuiListener chat switch**

After the `case "passive_amplifier"` block, add:
```java
            case "recipe_shape" -> {
                String[] rows = input.split(",");
                if (rows.length != 3) {
                    player.sendMessage("§c需要3行，用逗号分隔");
                    return;
                }
                if (def.getRecipe() == null) def.setRecipe(new RecipeConfig());
                def.getRecipe().setShape(Arrays.asList(rows));
                player.sendMessage(messages.get("editor.shape-set"));
            }
            case "recipe_ingredients" -> {
                String[] pairs = input.split(",");
                Map<String, String> ingredients = new HashMap<>();
                for (String pair : pairs) {
                    String[] parts = pair.split("=");
                    if (parts.length == 2) {
                        ingredients.put(parts[0].trim().toUpperCase(), parts[1].trim().toUpperCase());
                    }
                }
                if (def.getRecipe() == null) def.setRecipe(new RecipeConfig());
                def.getRecipe().setIngredients(ingredients);
                player.sendMessage(messages.get("editor.ingredients-set"));
            }
```

Add imports:
```java
import org.mineUGC.core.model.RecipeConfig;
```

- [ ] **Replace Recipe button in ItemEditorInventory**

Find:
```java
        setItem(23, createItem(Material.CRAFTING_TABLE, m.get("editor.field-recipe"), m.get("editor.click-configure")),
                e -> player.sendMessage(m.get("editor.coming-soon", m.get("editor.field-recipe"))));
```

Replace with:
```java
        setItem(23, createItem(Material.CRAFTING_TABLE, m.get("editor.field-recipe"), m.get("editor.click-configure")),
                e -> new RecipeInventory(player, def, itemManager, guiListener).open(player));
```

- [ ] **Commit**

```bash
git add gui/src/main/java/org/mineUGC/gui/editor/GuiListener.java gui/src/main/java/org/mineUGC/gui/editor/ItemEditorInventory.java
git commit -m "feat(editor): wire up recipe editing"
```

---

### Task 13: Build and deploy

**Files:**
- All above

- [ ] **Full project compile check**

```bash
./gradlew test compileJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Package and deploy**

```bash
./gradlew :plugin:jar
cp plugin/build/libs/plugin-1.0-SNAPSHOT.jar run/plugins/
```

- [ ] **Commit all changes**

```bash
git add -A
git commit -m "feat(editor): implement all remaining editor features"
```

---

## Self-Review Checklist

1. **Spec coverage:** All 5 features from the spec are covered (Set Bonus via chat, Attributes via sub-inventory, Abilities/Passives via 2-level sub-inventories, Recipe via sub-inventory).
2. **Placeholder scan:** No TBDs, TODOs, or vague steps. Every code block is complete.
3. **Type consistency:** AbilityConfig/PassiveConfig/RecipeConfig types match the model classes. Method signatures are consistent.
4. **Edge cases handled:** Empty maps show "none" messages. Duplicate keys are rejected. Invalid number input shows error. Recipe shape validates 3 rows.
