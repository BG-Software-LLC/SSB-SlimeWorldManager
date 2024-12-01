package com.bgsoftware.ssbslimeworldmanager.api;

import com.bgsoftware.superiorskyblock.api.objects.Enumerable;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.function.Function;

public class EnumerateMap<K extends Enumerable, V> {

    private static final Object[] EMPTY_VALUES = new Object[0];

    private Object[] values = EMPTY_VALUES;

    public EnumerateMap() {

    }

    @Nullable
    public V get(K key) {
        int idx = key.ordinal();
        if (idx < 0 || idx >= values.length)
            return null;
        return (V) values[idx];
    }

    @Nullable
    public V set(K key, V value) {
        int idx = key.ordinal();
        ensureCapacity(idx);
        V old = (V) this.values[idx];
        this.values[idx] = value;
        return old;
    }

    public V computeIfAbsent(K key, Function<K, V> mapper) {
        V value = get(key);
        if (value == null) {
            value = mapper.apply(key);
            set(key, value);
        }
        return value;
    }

    private void ensureCapacity(int index) {
        if (index >= values.length) {
            values = Arrays.copyOf(values, index + 1);
        }
    }

}
