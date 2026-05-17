package org.mineUGC.core.registry;

import org.mineUGC.core.model.UgcAsset;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class AssetRegistry<T extends UgcAsset> {
    private final ConcurrentHashMap<String, T> assets = new ConcurrentHashMap<>();

    public void register(T asset) {
        T existing = assets.putIfAbsent(asset.getId(), asset);
        if (existing != null) {
            throw new IllegalStateException("Asset already registered: " + asset.getId());
        }
    }

    public T replace(String id, T asset) { return assets.put(id, asset); }
    public T get(String id) { return assets.get(id); }
    public T remove(String id) { return assets.remove(id); }
    public boolean contains(String id) { return assets.containsKey(id); }
    public Collection<T> getAll() { return assets.values(); }
    public int size() { return assets.size(); }
    public void clear() { assets.clear(); }
}
