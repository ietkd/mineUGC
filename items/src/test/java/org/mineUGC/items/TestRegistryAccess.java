package org.mineUGC.items;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.mockito.Mockito;

import java.util.Collections;

/**
 * Test-only implementation of {@link RegistryAccess} used by the ServiceLoader
 * to enable loading of the {@link org.bukkit.attribute.Attribute} interface and
 * {@link org.bukkit.Registry} outside a running Paper server.
 *
 * <p>When the static initialisers of {@code Registry} and {@code Attribute} run
 * during test bootstrapping, this provider returns a mock {@link Registry} that
 * uses {@link Mockito#RETURNS_MOCKS} for most types (avoiding the need to load
 * any specific class, which would trigger Paper-internal static initialisers).
 * For {@code Attribute} specifically, the {@code get(...)} lookup methods are
 * stubbed to return a type-accurate mock so that the cast inside
 * {@code Attribute.getAttribute()} succeeds.</p>
 */
public class TestRegistryAccess implements RegistryAccess {

    @Override
    @SuppressWarnings({"unchecked", "deprecation"})
    public <T extends Keyed> Registry<T> getRegistry(final Class<T> type) {
        return buildMockRegistry(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Keyed> Registry<T> getRegistry(final RegistryKey<T> key) {
        // Not called during Registry.<clinit> — Paper uses the Class<?> overload.
        return (Registry<T>) Mockito.mock(Registry.class);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Keyed> Registry<T> buildMockRegistry(final Class<T> type) {
        /*
         * RETURNS_MOCKS is crucial here: it makes the mock return mock objects
         * for every method without ever trying to load / initialise the erased
         * return type (Keyed).  Loading some types (e.g. Biome) would trigger
         * Paper internal APIs that are not available in a unit test.
         */
        final Registry<T> registry = Mockito.mock(Registry.class, Mockito.RETURNS_MOCKS);

        // Attribute is special: its static initialiser casts the result of
        // Registry.ATTRIBUTE.getOrThrow(Key) to Attribute (see the bytecode of
        // Attribute.getAttribute(String)), so we must return a mock that IS-A
        // Attribute, not just a plain Keyed mock.
        if ("org.bukkit.attribute.Attribute".equals(type.getName())) {
            final T mockValue = Mockito.mock(type);
            Mockito.when(registry.get(Mockito.any(NamespacedKey.class))).thenReturn(mockValue);
            Mockito.when(registry.get(Mockito.any(Key.class))).thenReturn(mockValue);
            Mockito.when(registry.getOrThrow(Mockito.any(Key.class))).thenReturn(mockValue);
        }

        // Safe iteration defaults (RETURNS_MOCKS would return mock iterators
        // whose hasNext() returns false by default, but being explicit is safer).
        Mockito.when(registry.iterator()).thenReturn(Collections.emptyIterator());
        Mockito.when(registry.stream()).thenReturn(java.util.stream.Stream.empty());

        return registry;
    }
}
