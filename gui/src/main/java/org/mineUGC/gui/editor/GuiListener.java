package org.mineUGC.gui.editor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.items.ItemManager;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GuiListener implements Listener {
    private final Plugin plugin;
    private final ItemManager itemManager;
    private final File itemsDirectory;
    private final Map<UUID, EditSession> editSessions = new ConcurrentHashMap<>();

    public GuiListener(Plugin plugin, ItemManager itemManager, File itemsDirectory) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.itemsDirectory = itemsDirectory;
    }

    ItemManager getItemManager() {
        return itemManager;
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

    void promptField(Player player, String field, String message) {
        EditSession session = editSessions.get(player.getUniqueId());
        if (session == null) return;
        session.setPendingField(field);
        player.closeInventory();
        player.sendMessage("§e" + message);
        player.sendMessage("§7Type 'cancel' to abort.");
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
            player.sendMessage("§cCancelled.");
            return;
        }

        String field = session.getPendingField();
        ItemDefinition def = session.getDefinition();

        switch (field) {
            case "name" -> {
                def.setName(input);
                player.sendMessage("§aName set to: §f" + input);
            }
            case "material" -> {
                def.setMaterial(input.toUpperCase());
                player.sendMessage("§aMaterial set to: §f" + input.toUpperCase());
            }
            case "model" -> {
                try {
                    int model = Integer.parseInt(input);
                    def.setModel(model);
                    player.sendMessage("§aModel data set to: §f" + model);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid number. Use /ugc edit to try again.");
                    return;
                }
            }
            case "lore" -> {
                String[] lines = input.split("\\|");
                def.setLore(Arrays.asList(lines));
                player.sendMessage("§aLore set (" + lines.length + " lines)");
            }
        }

        session.setPendingField(null);
        reopenEditor(player, session);
    }

    private void reopenEditor(Player player, EditSession session) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            new ItemEditorInventory(player, session.getDefinition(), itemManager, this).open(player);
        });
    }
}
