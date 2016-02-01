/**
 * Copyright (c) 2015-2016 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jmnarloch.concurrent.cache;

import com.google.common.cache.CacheBuilder;

import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A {@link Cache} that evicts it's entries after configurable amount of time.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @author Jakub Narloch
 */
class EvictableCache<K, V> implements Cache<K, V> {

    /**
     * Delegated cache.
     */
    private ConcurrentMap<K, V> cache;

    /**
     * Creates new instance of {@link EvictableCache}.
     *
     * @param duration the duration after which the entries will be evicted
     * @param unit     the time unit
     */
    public EvictableCache(long duration, TimeUnit unit) {

        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(duration, unit)
                .<K, V>build()
                .asMap();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long size() {
        return cache.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(K key, V value) {
        cache.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putIfAbsent(K key, V value) {
        cache.putIfAbsent(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V computeIfAbsent(K key, Supplier<V> supplier) {
        return cache.computeIfAbsent(key, (k) -> supplier.get());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V get(K key) {
        return cache.get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<V> getOptional(K key) {
        return Optional.ofNullable(get(key));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V remove(K key) {
        return cache.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return cache.replace(key, oldValue, newValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invalidateIfPresent(K key, Consumer<V> consumer) {

        final V value = cache.remove(key);
        if(value != null) {
            consumer.accept(value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invalidateAll() {
        cache.clear();
    }
}
