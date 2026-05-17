package org.mineUGC.items.attributes;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttributeApplierTest {

    private AttributeApplier applier;
    private Map<String, Attribute> testAttributeMap;

    @Mock
    private ItemStack item;

    @Mock
    private ItemMeta meta;

    @Captor
    private ArgumentCaptor<AttributeModifier> modifierCaptor;

    /**
     * Creates a mock Attribute via java.lang.reflect.Proxy to avoid triggering
     * Attribute.&lt;clinit&gt; (which requires a running Paper server).
     */
    @SuppressWarnings("unchecked")
    private static Attribute createMockAttribute() {
        try {
            Class<?> attrInterface = Class.forName("org.bukkit.attribute.Attribute", false,
                    AttributeApplierTest.class.getClassLoader());
            return (Attribute) Proxy.newProxyInstance(
                    attrInterface.getClassLoader(),
                    new Class<?>[]{attrInterface},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        if ("name".equals(name)) return "mock_attr";
                        if ("ordinal".equals(name)) return 0;
                        if ("toString".equals(name)) return "mock_attr";
                        if ("hashCode".equals(name)) return 0;
                        return null;
                    }
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create mock Attribute", e);
        }
    }

    @BeforeEach
    void setUp() {
        testAttributeMap = new HashMap<>();
        testAttributeMap.put("damage", createMockAttribute());
        testAttributeMap.put("speed", createMockAttribute());
        testAttributeMap.put("armor", createMockAttribute());
        testAttributeMap.put("armor_toughness", createMockAttribute());
        testAttributeMap.put("movement_speed", createMockAttribute());
        testAttributeMap.put("max_health", createMockAttribute());
        testAttributeMap.put("knockback_resistance", createMockAttribute());
        testAttributeMap.put("luck", createMockAttribute());
        testAttributeMap.put("block_interaction_range", createMockAttribute());
        testAttributeMap.put("entity_interaction_range", createMockAttribute());
        applier = new AttributeApplier(testAttributeMap);
    }

    @Test
    void apply_nullAttributes_doesNothing() {
        applier.apply(item, null);

        verify(item, never()).getItemMeta();
    }

    @Test
    void apply_emptyAttributes_doesNothing() {
        applier.apply(item, Map.of());

        verify(item, never()).getItemMeta();
    }

    @Test
    void apply_unknownAttributeKey_skipsGracefully() {
        when(item.getItemMeta()).thenReturn(meta);

        applier.apply(item, Map.of("unknown_attr", 5.0));

        verify(meta, never()).addAttributeModifier(any(Attribute.class), any(AttributeModifier.class));
    }

    @Test
    void apply_validAttribute_appliesModifier() {
        when(item.getItemMeta()).thenReturn(meta);
        when(meta.getAttributeModifiers(any(Attribute.class))).thenReturn(null);

        applier.apply(item, Map.of("damage", 10.0));

        verify(meta).addAttributeModifier(any(Attribute.class), modifierCaptor.capture());
        AttributeModifier modifier = modifierCaptor.getValue();
        assertEquals(10.0, modifier.getAmount(), 0.001);
        assertEquals("ugc_damage", modifier.getName());
    }

    @Test
    void apply_replacesExistingModifierWithSameName() {
        AttributeModifier existingMod = mock(AttributeModifier.class);
        when(existingMod.getName()).thenReturn("ugc_damage");

        when(item.getItemMeta()).thenReturn(meta);
        when(meta.getAttributeModifiers(any(Attribute.class)))
                .thenReturn(List.of(existingMod));

        applier.apply(item, Map.of("damage", 5.0));

        verify(meta).removeAttributeModifier(any(Attribute.class), eq(existingMod));
        verify(meta).addAttributeModifier(any(Attribute.class), any(AttributeModifier.class));
    }

    @Test
    void apply_itemMetaNull_doesNothing() {
        when(item.getItemMeta()).thenReturn(null);

        applier.apply(item, Map.of("damage", 10.0));

        verify(meta, never()).addAttributeModifier(any(Attribute.class), any(AttributeModifier.class));
    }

    @Test
    void attributeMap_coversAllKnownKeys() {
        Map<String, Double> allAttrs = new HashMap<>();
        allAttrs.put("damage", 1.0);
        allAttrs.put("speed", 1.0);
        allAttrs.put("armor", 1.0);
        allAttrs.put("armor_toughness", 1.0);
        allAttrs.put("movement_speed", 1.0);
        allAttrs.put("max_health", 1.0);
        allAttrs.put("knockback_resistance", 1.0);
        allAttrs.put("luck", 1.0);
        allAttrs.put("block_interaction_range", 1.0);
        allAttrs.put("entity_interaction_range", 1.0);

        when(item.getItemMeta()).thenReturn(meta);
        when(meta.getAttributeModifiers(any(Attribute.class))).thenReturn(null);

        applier.apply(item, allAttrs);

        verify(meta, times(10)).addAttributeModifier(any(Attribute.class), any(AttributeModifier.class));
    }
}
