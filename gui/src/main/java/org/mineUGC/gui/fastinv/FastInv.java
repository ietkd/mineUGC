package org.mineUGC.gui.fastinv;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Consumer;

public class FastInv implements InventoryHolder {
    private final Map<Integer, Consumer<InventoryClickEvent>> itemHandlers = new HashMap<>();
    private final List<Consumer<InventoryCloseEvent>> closeHandlers = new ArrayList<>();
    private final List<Consumer<InventoryClickEvent>> clickHandlers = new ArrayList<>();
    private final Inventory inventory;

    public FastInv(int size, String title) {
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    protected void onClick(InventoryClickEvent event) {}
    protected void onClose(InventoryCloseEvent event) {}

    public void setItem(int slot, ItemStack item) {
        setItem(slot, item, null);
    }

    public void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> handler) {
        inventory.setItem(slot, item);
        if (handler != null) {
            itemHandlers.put(slot, handler);
        } else {
            itemHandlers.remove(slot);
        }
    }

    public void addCloseHandler(Consumer<InventoryCloseEvent> closeHandler) {
        this.closeHandlers.add(closeHandler);
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    void handleClick(InventoryClickEvent e) {
        onClick(e);
        clickHandlers.forEach(c -> c.accept(e));
        Consumer<InventoryClickEvent> handler = itemHandlers.get(e.getRawSlot());
        if (handler != null) {
            handler.accept(e);
        }
    }

    void handleClose(InventoryCloseEvent e) {
        onClose(e);
        closeHandlers.forEach(c -> c.accept(e));
    }
}
