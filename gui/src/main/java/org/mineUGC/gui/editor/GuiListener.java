package org.mineUGC.gui.editor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;
import org.mineUGC.core.message.Messages;
import org.mineUGC.core.model.AbilityConfig;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.items.ItemManager;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class GuiListener implements Listener {
    private final Plugin plugin;
    private final ItemManager itemManager;
    private final File itemsDirectory;
    private final Messages messages;
    private final Map<UUID, EditSession> editSessions = new ConcurrentHashMap<>();

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

    void promptField(Player player, String field, String message) {
        EditSession session = editSessions.get(player.getUniqueId());
        if (session == null) return;
        session.setPendingField(field);
        player.closeInventory();
        player.sendMessage("§e" + message);
        player.sendMessage(messages.get("editor.cancel-hint"));
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        EditSession session = editSessions.get(player.getUniqueId());
        if (session == null || session.getPendingField() == null) return;

        event.setCancelled(true);
        String input = event.getMessage().trim();

        if (input.equalsIgnoreCase("cancel")) {
            session.setPendingField(null);
            reopenEditor(player, session);
            player.sendMessage(messages.get("editor.cancelled"));
            return;
        }

        String field = session.getPendingField();
        ItemDefinition def = session.getDefinition();

        switch (field) {
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
        }

        session.setPendingField(null);
        reopenEditor(player, session);
    }

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
}
