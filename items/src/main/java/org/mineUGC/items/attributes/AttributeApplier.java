package org.mineUGC.items.attributes;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class AttributeApplier {

    private final Map<String, Attribute> attributeMap;

    public AttributeApplier() {
        this.attributeMap = buildDefaultMap();
    }

    AttributeApplier(Map<String, Attribute> attributeMap) {
        this.attributeMap = attributeMap;
    }

    private static Map<String, Attribute> buildDefaultMap() {
        Map<String, Attribute> map = new HashMap<>();
        // Use reflection to access Attribute static fields, which avoids
        // bytecode-level linkage issues on certain Paper 1.21.1 builds.
        put(map, "damage", "ATTACK_DAMAGE", "attack_damage");
        put(map, "speed", "ATTACK_SPEED", "attack_speed");
        put(map, "armor", "ARMOR", "armor");
        put(map, "armor_toughness", "ARMOR_TOUGHNESS", "armor_toughness");
        put(map, "movement_speed", "MOVEMENT_SPEED", "movement_speed");
        put(map, "max_health", "MAX_HEALTH", "max_health");
        put(map, "knockback_resistance", "KNOCKBACK_RESISTANCE", "knockback_resistance");
        put(map, "luck", "LUCK", "luck");
        put(map, "block_interaction_range", "BLOCK_INTERACTION_RANGE", "block_interaction_range");
        put(map, "entity_interaction_range", "ENTITY_INTERACTION_RANGE", "entity_interaction_range");
        return Map.copyOf(map);
    }

    private static void put(Map<String, Attribute> map, String key, String fieldName, String registryKey) {
        // Reflection first (avoids Paper remapping issues with direct field access)
        try {
            Attribute attr = (Attribute) Attribute.class.getField(fieldName).get(null);
            map.put(key, attr);
            return;
        } catch (Exception ignored) {
        }
        // Fallback: registry lookup
        try {
            Attribute attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(registryKey));
            if (attr != null) {
                map.put(key, attr);
                return;
            }
        } catch (Exception ignored) {
        }
    }

    public void apply(ItemStack item, Map<String, Double> attributes) {
        if (attributes == null || attributes.isEmpty()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        for (Map.Entry<String, Double> entry : attributes.entrySet()) {
            Attribute attr = attributeMap.get(entry.getKey());
            if (attr == null) continue;

            if (meta.getAttributeModifiers(attr) != null) {
                for (AttributeModifier mod : meta.getAttributeModifiers(attr)) {
                    if (mod.getName().equals("ugc_" + entry.getKey())) {
                        meta.removeAttributeModifier(attr, mod);
                    }
                }
            }

            AttributeModifier modifier = new AttributeModifier(
                    NamespacedKey.minecraft("ugc_" + entry.getKey()),
                    entry.getValue(),
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.MAINHAND
            );
            meta.addAttributeModifier(attr, modifier);
        }

        item.setItemMeta(meta);
    }
}
