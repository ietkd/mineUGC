package org.mineUGC.gui.fastinv;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.plugin.Plugin;

public final class FastInvManager {

    private FastInvManager() {}

    public static void register(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new InventoryListener(), plugin);
    }

    private static final class InventoryListener implements Listener {

        @EventHandler
        public void onInventoryClick(InventoryClickEvent e) {
            if (e.getClickedInventory() != e.getView().getTopInventory()) return;
            if (e.getInventory().getHolder() instanceof FastInv inv) {
                e.setCancelled(true);
                inv.handleClick(e);
            }
        }

        @EventHandler
        public void onInventoryClose(InventoryCloseEvent e) {
            if (e.getInventory().getHolder() instanceof FastInv inv) {
                inv.handleClose(e);
            }
        }
    }
}
