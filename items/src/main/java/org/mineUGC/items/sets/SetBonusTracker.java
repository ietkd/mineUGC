package org.mineUGC.items.sets;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineUGC.items.ItemManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SetBonusTracker {
    private final ItemManager itemManager;
    private final Map<UUID, Map<String, Integer>> playerSetCounts = new ConcurrentHashMap<>();

    public SetBonusTracker(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    public Map<String, Integer> getSetCounts(Player player) {
        if (player == null) return Map.of();
        Map<String, Integer> counts = new HashMap<>();
        for (ItemStack item : player.getInventory().getArmorContents()) {
            String setId = getSetId(item);
            if (setId != null) {
                counts.merge(setId, 1, Integer::sum);
            }
        }
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        String mainSet = getSetId(mainHand);
        if (mainSet != null) counts.merge(mainSet, 1, Integer::sum);

        ItemStack offHand = player.getInventory().getItemInOffHand();
        String offSet = getSetId(offHand);
        if (offSet != null) counts.merge(offSet, 1, Integer::sum);

        return counts;
    }

    public int getPieceCount(Player player, String setId) {
        return getSetCounts(player).getOrDefault(setId, 0);
    }

    public boolean hasFullSet(Player player, String setId, int requiredPieces) {
        return getPieceCount(player, setId) >= requiredPieces;
    }

    private String getSetId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String itemId = itemManager.getItemId(item);
        if (itemId == null) return null;
        var def = itemManager.getDefinition(itemId);
        return def != null ? def.getSet() : null;
    }
}
