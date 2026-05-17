package org.mineUGC.items.abilities;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.mineUGC.core.model.AbilityConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

public class AbilityExecutor {
    private final Logger logger;
    private final Map<String, BiConsumer<Player, AbilityConfig>> handlers = new HashMap<>();

    public AbilityExecutor(Logger logger) {
        this.logger = logger;
        registerDefaults();
    }

    private void registerDefaults() {
        handlers.put("lightning_strike", (player, cfg) -> {
            Location target = player.getTargetBlock(null, 50).getLocation().add(0.5, 1, 0.5);
            player.getWorld().strikeLightning(target);
            double damage = cfg.param("damage", 8.0);
            target.getNearbyEntities(3, 3, 3).stream()
                    .filter(e -> e instanceof LivingEntity && e != player)
                    .forEach(e -> ((LivingEntity) e).damage(damage, player));
        });

        handlers.put("potion_effect", (player, cfg) -> {
            String effectName = cfg.param("effect", "speed");
            int amplifier = cfg.param("amplifier", 0);
            int duration = cfg.param("duration", 100);
            PotionEffectType type = PotionEffectType.getByName(effectName.toUpperCase());
            if (type != null) {
                player.addPotionEffect(new PotionEffect(type, duration, amplifier));
            }
        });

        handlers.put("projectile", (player, cfg) -> {
            String projectileType = cfg.param("projectile_type", "arrow");
            try {
                EntityType entityType = EntityType.valueOf(projectileType.toUpperCase());
                Class<?> entityClass = entityType.getEntityClass();
                if (entityClass != null && org.bukkit.entity.Projectile.class.isAssignableFrom(entityClass)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends org.bukkit.entity.Projectile> projectileClass =
                            (Class<? extends org.bukkit.entity.Projectile>) entityClass;
                    player.launchProjectile(projectileClass);
                } else {
                    logger.warning("Entity type is not a projectile: " + projectileType);
                }
            } catch (IllegalArgumentException e) {
                logger.warning("Unknown projectile type: " + projectileType);
            }
        });

        handlers.put("explosion", (player, cfg) -> {
            float power = cfg.param("power", 2.0F).floatValue();
            boolean fire = cfg.param("fire", false);
            player.getWorld().createExplosion(player.getLocation(), power, fire);
        });

        handlers.put("heal", (player, cfg) -> {
            double amount = cfg.param("amount", 4.0);
            player.setHealth(Math.min(player.getHealth() + amount, player.getMaxHealth()));
        });

        handlers.put("teleport", (player, cfg) -> {
            Location target = player.getTargetBlock(null, 50).getLocation().add(0.5, 1, 0.5);
            player.teleport(target);
        });

        handlers.put("particle_ring", (player, cfg) -> {
            String particleName = cfg.param("particle", "flame");
            int count = cfg.param("count", 20);
            double radius = cfg.param("radius", 2.0);
            try {
                Particle particle = Particle.valueOf(particleName.toUpperCase());
                Location center = player.getLocation();
                for (int i = 0; i < count; i++) {
                    double angle = 2 * Math.PI * i / count;
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    center.add(x, 0, z);
                    player.getWorld().spawnParticle(particle, center, 1, 0, 0, 0, 0);
                    center.subtract(x, 0, z);
                }
            } catch (IllegalArgumentException e) {
                logger.warning("Unknown particle type: " + particleName);
            }
        });
    }

    public void execute(Player player, AbilityConfig config) {
        if (config == null || config.getType() == null) return;
        BiConsumer<Player, AbilityConfig> handler = handlers.get(config.getType());
        if (handler != null) {
            try {
                handler.accept(player, config);
            } catch (Exception e) {
                logger.warning("Ability execution failed: " + config.getType() + " - " + e.getMessage());
            }
        } else {
            logger.warning("Unknown ability type: " + config.getType());
        }
    }

    public void registerHandler(String type, BiConsumer<Player, AbilityConfig> handler) {
        handlers.put(type, handler);
    }
}
