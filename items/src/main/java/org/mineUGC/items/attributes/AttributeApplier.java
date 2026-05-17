package org.mineUGC.items.attributes;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public class AttributeApplier {

    private final Map<String, Attribute> attributeMap;

    /** Public constructor -- builds the built-in attribute map. */
    public AttributeApplier() {
        this.attributeMap = buildDefaultMap();
    }

    /**
     * Package-private constructor for testing.  Accepts a custom attribute
     * map so that tests can inject mocked or null-valued attributes without
     * triggering the Paper {@link Attribute} static initialiser (which
     * requires a running Paper server).
     */
    AttributeApplier(Map<String, Attribute> attributeMap) {
        this.attributeMap = attributeMap;
    }

    private static Map<String, Attribute> buildDefaultMap() {
        return Map.ofEntries(
            Map.entry("damage", Attribute.ATTACK_DAMAGE),
            Map.entry("speed", Attribute.ATTACK_SPEED),
            Map.entry("armor", Attribute.ARMOR),
            Map.entry("armor_toughness", Attribute.ARMOR_TOUGHNESS),
            Map.entry("movement_speed", Attribute.MOVEMENT_SPEED),
            Map.entry("max_health", Attribute.MAX_HEALTH),
            Map.entry("knockback_resistance", Attribute.KNOCKBACK_RESISTANCE),
            Map.entry("luck", Attribute.LUCK),
            Map.entry("block_interaction_range", Attribute.BLOCK_INTERACTION_RANGE),
            Map.entry("entity_interaction_range", Attribute.ENTITY_INTERACTION_RANGE)
        );
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
                    new NamespacedKey("minecraft", "ugc_" + entry.getKey()),
                    entry.getValue(),
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.MAINHAND
            );
            meta.addAttributeModifier(attr, modifier);
        }

        item.setItemMeta(meta);
    }
}
