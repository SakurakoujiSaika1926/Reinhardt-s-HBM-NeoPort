package com.reinhardt.hbm.client.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * Weak client-only index used to atomically republish immutable render snapshots
 * when a model bake replaces the current resource generation.
 */
final class ClientRenderSnapshotInstances<T> {
    private final Set<T> instances = Collections.newSetFromMap(new WeakHashMap<>());

    void track(T instance) {
        synchronized (instances) {
            instances.add(instance);
        }
    }

    void refreshAll(Consumer<T> refresher) {
        ArrayList<T> loaded;
        synchronized (instances) {
            loaded = new ArrayList<>(instances);
        }
        loaded.forEach(refresher);
    }

    void clear() {
        synchronized (instances) {
            instances.clear();
        }
    }
}
