package org.mineUGC.items.passives;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.mineUGC.core.model.PassiveConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class PassiveEffect {
    private final Map<String, BiConsumer<Player, PassiveConfig>> handlers = new HashMap<>();

    public PassiveEffect() {
        registerDefaults();
    }

    private void registerDefaults() {
        handlers.put("potion_effect", (player, cfg) -> {
            String effectName = cfg.getEffect();
            if (effectName == null) return;
            PotionEffectType type = PotionEffectType.getByName(effectName.toUpperCase());
            if (type != null) {
                int amplifier = cfg.getAmplifier();
                player.addPotionEffect(new PotionEffect(type, 200, amplifier, true, false, true));
            }
        });
    }

    public void apply(Player player, PassiveConfig config) {
        if (config == null || config.getType() == null) return;
        BiConsumer<Player, PassiveConfig> handler = handlers.get(config.getType());
        if (handler != null) {
            handler.accept(player, config);
        }
    }

    public void registerHandler(String type, BiConsumer<Player, PassiveConfig> handler) {
        handlers.put(type, handler);
    }
}
