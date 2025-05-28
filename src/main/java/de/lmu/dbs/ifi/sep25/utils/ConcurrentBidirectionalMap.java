package de.lmu.dbs.ifi.sep25.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConcurrentBidirectionalMap<K, V> {
    private final ConcurrentHashMap<K, V> forward = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<V, K> reverse = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public void put(K key, V value) {
        lock.writeLock().lock();
        try {
            V oldValue = forward.put(key, value);
            if (oldValue != null) {
                reverse.remove(oldValue);
            }

            K oldKey = reverse.put(value, key);
            if (oldKey != null) {
                forward.remove(oldKey);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public V getByKey(K key) {
        return forward.get(key);
    }

    public K getByValue(V value) {
        return reverse.get(value);
    }

    public void removeByKey(K key) {
        lock.writeLock().lock();
        try {
            V value = forward.remove(key);
            if (value != null) {
                reverse.remove(value);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeByValue(V value) {
        lock.writeLock().lock();
        try {
            K key = reverse.remove(value);
            if (key != null) {
                forward.remove(key);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean containsKey(K key) {
        return forward.containsKey(key);
    }

    public boolean containsValue(V value) {
        return reverse.containsKey(value);
    }

    public int size() {
        return forward.size();
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            forward.clear();
            reverse.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Set<K> keySet() {
        return new HashSet<>(forward.keySet());
    }


}
