package org.mineUGC.gui.editor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.mineUGC.core.message.Messages;
import org.mineUGC.core.model.AbilityConfig;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.core.model.PassiveConfig;
import org.mineUGC.core.model.RecipeConfig;
import org.mineUGC.items.ItemManager;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GuiListener implements Listener {
    private final Plugin plugin;
    private final ItemManager itemManager;
    private final File itemsDirectory;
    private final Messages messages;
    private final Map<UUID, EditSession> editSessions = new ConcurrentHashMap<>();
    private final Set<UUID> activeAnvilPlayers = ConcurrentHashMap.newKeySet();

    public GuiListener(Plugin plugin, ItemManager itemManager, File itemsDirectory, Messages messages) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.itemsDirectory = itemsDirectory;
        this.messages = messages;
    }

    ItemManager getItemManager() {
        return itemManager;
    }

    Messages getMessages() {
        return messages;
    }

    File getItemsDirectory() {
        return itemsDirectory;
    }

    void startSession(UUID playerId, ItemDefinition def, boolean existing) {
        editSessions.put(playerId, new EditSession(def, existing));
    }

    void endSession(UUID playerId) {
        editSessions.remove(playerId);
    }

    Optional<EditSession> getEditSession(UUID playerId) {
        return Optional.ofNullable(editSessions.get(playerId));
    }

    // === Anvil-based text input ===

    void openAnvilInput(Player player, String field) {
        EditSession session = editSessions.get(player.getUniqueId());
        if (session == null) return;

        String currentValue = getCurrentFieldValue(session.getDefinition(), session, field);
        session.setPendingField(field);

        player.closeInventory();

        activeAnvilPlayers.add(player.getUniqueId());

        var anvil = Bukkit.createInventory(player, InventoryType.ANVIL, "§8Edit " + field);

        ItemStack input = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = input.getItemMeta();

        String placeholder = getFieldPlaceholder(field);
        meta.setDisplayName(currentValue != null ? currentValue : "§7" + placeholder);
        meta.setLore(List.of("§7Type the new value in the rename field,", "§7then click the output item."));
        input.setItemMeta(meta);
        anvil.setItem(0, input);
        anvil.setItem(1, new ItemStack(Material.AIR));

        player.openInventory(anvil);
        player.setLevel(100);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!activeAnvilPlayers.contains(player.getUniqueId())) return;
        if (event.getRawSlot() != 2) return;

        event.setCancelled(true);

        ItemStack result = event.getCurrentItem();
        if (result == null || !result.hasItemMeta()) return;

        String input = result.getItemMeta().getDisplayName();
        if (input == null || input.isEmpty()) return;

        String rawInput = input.replaceAll("§[0-9a-fklmnor]", "").trim();

        if (rawInput.equalsIgnoreCase("cancel")) {
            activeAnvilPlayers.remove(player.getUniqueId());
            getEditSession(player.getUniqueId()).ifPresent(s -> s.setPendingField(null));
            reopenEditor(player);
            player.sendMessage(messages.get("editor.cancelled"));
            return;
        }

        processFieldInput(player, rawInput);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!activeAnvilPlayers.remove(player.getUniqueId())) return;

        getEditSession(player.getUniqueId()).ifPresent(session -> {
            if (session.getPendingField() != null) {
                session.setPendingField(null);
                reopenEditor(player);
            }
        });
    }

    private void processFieldInput(Player player, String input) {
        EditSession session = editSessions.get(player.getUniqueId());
        if (session == null) return;

        String field = session.getPendingField();
        ItemDefinition def = session.getDefinition();

        switch (field) {
            // === ItemEditorInventory fields ===
            case "name" -> {
                def.setName(input);
                player.sendMessage(messages.get("editor.name-set", input));
            }
            case "material" -> {
                def.setMaterial(input.toUpperCase());
                player.sendMessage(messages.get("editor.material-set", input.toUpperCase()));
            }
            case "model" -> {
                try {
                    int model = Integer.parseInt(input);
                    def.setModel(model);
                    player.sendMessage(messages.get("editor.model-set", String.valueOf(model)));
                } catch (NumberFormatException e) {
                    player.sendMessage(messages.get("editor.invalid-number"));
                    return;
                }
            }
            case "lore" -> {
                String[] lines = input.split("\\|");
                def.setLore(Arrays.asList(lines));
                player.sendMessage(messages.get("editor.lore-set", lines.length));
            }
            case "id" -> {
                String sanitized = input.toLowerCase().replaceAll("[^a-z0-9_/]", "_");
                def.setId(sanitized);
                player.sendMessage(messages.get("editor.id-set", sanitized));
            }
            case "set" -> {
                def.setSet(input);
                player.sendMessage(messages.get("editor.set-set", input));
            }

            // === Attributes ===
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

            // === Abilities ===
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

            // === Passives ===
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

            // === Recipe ===
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

            default -> player.sendMessage("§cUnknown field: " + field);
        }

        session.setPendingField(null);
        activeAnvilPlayers.remove(player.getUniqueId());
        reopenEditor(player);
    }

    private static String getCurrentFieldValue(ItemDefinition def, EditSession session, String field) {
        return switch (field) {
            case "name" -> def.getName();
            case "material" -> def.getMaterial();
            case "model" -> def.getModel() > 0 ? String.valueOf(def.getModel()) : null;
            case "lore" -> def.getLore() != null ? String.join("|", def.getLore()) : null;
            case "id" -> def.getId();
            case "set" -> def.getSet();
            case "ability_type" -> {
                String k = session.getEditingAbilityKey();
                if (k != null && def.getAbilities() != null && def.getAbilities().get(k) != null)
                    yield def.getAbilities().get(k).getType();
                yield null;
            }
            case "ability_cooldown" -> {
                String k = session.getEditingAbilityKey();
                if (k != null && def.getAbilities() != null && def.getAbilities().get(k) != null)
                    yield String.valueOf(def.getAbilities().get(k).getCooldown());
                yield null;
            }
            case "ability_mana" -> {
                String k = session.getEditingAbilityKey();
                if (k != null && def.getAbilities() != null && def.getAbilities().get(k) != null)
                    yield String.valueOf(def.getAbilities().get(k).getManaCost());
                yield null;
            }
            case "passive_type" -> {
                String k = session.getEditingPassiveKey();
                if (k != null && def.getPassives() != null && def.getPassives().get(k) != null)
                    yield def.getPassives().get(k).getType();
                yield null;
            }
            case "passive_effect" -> {
                String k = session.getEditingPassiveKey();
                if (k != null && def.getPassives() != null && def.getPassives().get(k) != null)
                    yield def.getPassives().get(k).getEffect();
                yield null;
            }
            case "passive_amplifier" -> {
                String k = session.getEditingPassiveKey();
                if (k != null && def.getPassives() != null && def.getPassives().get(k) != null)
                    yield String.valueOf(def.getPassives().get(k).getAmplifier());
                yield null;
            }
            default -> null;
        };
    }

    private static String getFieldPlaceholder(String field) {
        return switch (field) {
            case "name" -> "Enter display name (use & for color)";
            case "material" -> "Enter material (e.g. DIAMOND_SWORD)";
            case "model" -> "Enter custom model data number";
            case "lore" -> "Enter lore lines separated by |";
            case "id" -> "Enter item ID (lowercase, underscore)";
            case "set" -> "Enter set name";
            case "attribute_add" -> "attribute value (e.g. damage 5.0)";
            case "ability_add" -> "Enter ability key (e.g. right_click)";
            case "ability_type" -> "Enter ability type (e.g. projectile)";
            case "ability_cooldown" -> "Enter cooldown in seconds";
            case "ability_mana" -> "Enter mana cost";
            case "passive_add" -> "Enter passive key (e.g. on_tick)";
            case "passive_type" -> "Enter passive type";
            case "passive_effect" -> "Enter effect (e.g. speed)";
            case "passive_amplifier" -> "Enter amplifier number";
            case "recipe_shape" -> "3 rows with commas (e.g. DDD,D D, S)";
            case "recipe_ingredients" -> "Mappings with commas (e.g. D=DIAMOND,S=STICK)";
            default -> "Enter value";
        };
    }

    private void reopenEditor(Player player) {
        getEditSession(player.getUniqueId()).ifPresent(session -> {
            var action = session.getReopenAction();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (action != null) {
                    action.accept(player, this);
                } else {
                    new ItemEditorInventory(player, session.getDefinition(), itemManager, this).open(player);
                }
            });
        });
    }
}
