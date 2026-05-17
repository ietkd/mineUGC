package org.mineUGC.core.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mineUGC.core.model.UgcAsset;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class AssetRegistryTest {

    private AssetRegistry<TestAsset> registry;

    static class TestAsset implements UgcAsset {
        private final String id;

        TestAsset(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getType() {
            return "test";
        }
    }

    @BeforeEach
    void setUp() {
        registry = new AssetRegistry<>();
    }

    @Test
    void register_addsAsset() {
        TestAsset asset = new TestAsset("sword");
        registry.register(asset);
        assertSame(asset, registry.get("sword"));
    }

    @Test
    void register_duplicateThrows() {
        registry.register(new TestAsset("sword"));
        assertThrows(IllegalStateException.class,
                () -> registry.register(new TestAsset("sword")));
    }

    @Test
    void replace_overwritesExisting() {
        TestAsset old = new TestAsset("sword");
        TestAsset updated = new TestAsset("sword");
        registry.register(old);

        TestAsset returned = registry.replace("sword", updated);
        assertSame(old, returned);
        assertSame(updated, registry.get("sword"));
    }

    @Test
    void replace_missingReturnsNull() {
        assertNull(registry.replace("nonexistent", new TestAsset("nonexistent")));
    }

    @Test
    void get_returnsNullForMissing() {
        assertNull(registry.get("missing"));
    }

    @Test
    void remove_deletesAsset() {
        TestAsset asset = new TestAsset("sword");
        registry.register(asset);

        TestAsset removed = registry.remove("sword");
        assertSame(asset, removed);
        assertNull(registry.get("sword"));
    }

    @Test
    void getAll_returnsAllRegistered() {
        TestAsset a = new TestAsset("a");
        TestAsset b = new TestAsset("b");
        registry.register(a);
        registry.register(b);

        Collection<TestAsset> all = registry.getAll();
        assertEquals(2, all.size());
        assertTrue(all.contains(a));
        assertTrue(all.contains(b));
    }

    @Test
    void size_reflectsRegistrationCount() {
        assertEquals(0, registry.size());
        registry.register(new TestAsset("a"));
        assertEquals(1, registry.size());
        registry.register(new TestAsset("b"));
        assertEquals(2, registry.size());
    }

    @Test
    void clear_emptiesRegistry() {
        registry.register(new TestAsset("a"));
        registry.register(new TestAsset("b"));
        registry.clear();
        assertEquals(0, registry.size());
        assertNull(registry.get("a"));
        assertNull(registry.get("b"));
    }

    @Test
    void contains_returnsTrueForRegistered() {
        registry.register(new TestAsset("sword"));
        assertTrue(registry.contains("sword"));
        assertFalse(registry.contains("missing"));
    }

    @Test
    void register_multipleDistinctIdsAllAccessible() {
        TestAsset a = new TestAsset("a");
        TestAsset b = new TestAsset("b");
        TestAsset c = new TestAsset("c");
        registry.register(a);
        registry.register(b);
        registry.register(c);
        assertSame(a, registry.get("a"));
        assertSame(b, registry.get("b"));
        assertSame(c, registry.get("c"));
    }
}
