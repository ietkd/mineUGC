package org.mineUGC.items.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.mineUGC.core.model.AbilityConfig;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.items.ItemManager;
import org.mineUGC.items.abilities.AbilityExecutor;
import org.mineUGC.storage.sqlite.PlayerDataDAO;

import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

public class ItemListener implements Listener {
    private final ItemManager itemManager;
    private final AbilityExecutor abilityExecutor;
    private final PlayerDataDAO playerDataDAO;
    private final Logger logger;

    public ItemListener(ItemManager itemManager, AbilityExecutor abilityExecutor,
                        PlayerDataDAO playerDataDAO, Logger logger) {
        this.itemManager = itemManager;
        this.abilityExecutor = abilityExecutor;
        this.playerDataDAO = playerDataDAO;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        String itemId = itemManager.getItemId(item);
        if (itemId == null) return;

        ItemDefinition def = itemManager.getDefinition(itemId);
        if (def == null || def.getAbilities() == null) return;

        AbilityConfig ability = def.getAbilities().get("right_click");
        if (ability == null) return;

        // Check cooldown
        UUID playerId = player.getUniqueId();
        try {
            long expiry = playerDataDAO.getCooldownExpiry(playerId, itemId, "right_click");
            if (expiry > System.currentTimeMillis()) {
                player.sendMessage("§cAbility on cooldown.");
                return;
            }
        } catch (SQLException e) {
            logger.warning("Failed to check cooldown: " + e.getMessage());
        }

        abilityExecutor.execute(player, ability);

        // Set cooldown
        if (ability.getCooldown() > 0) {
            try {
                long expiresAt = System.currentTimeMillis() + (ability.getCooldown() * 1000L);
                playerDataDAO.setCooldown(playerId, itemId, "right_click", expiresAt);
            } catch (SQLException e) {
                logger.warning("Failed to set cooldown: " + e.getMessage());
            }
        }

        event.setCancelled(true);
    }
}
