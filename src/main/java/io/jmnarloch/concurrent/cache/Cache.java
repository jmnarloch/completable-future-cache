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

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An abstraction of simple thread safe cache.
 *
 * Thread safety: this class requires to be able at least to perform atomic operations on individual keys,
 * with respect to some reasonable granulation, that is without blocking access to entire data structure.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @author Jakub Narloch
 */
interface Cache<K, V> {

    /**
     * Returns whether the cache is empty.
     *
     * @return true if cache is empty
     */
    boolean isEmpty();

    /**
     * Returns the number of entries stored in the cache.
     *
     * @return the number of entries in the cache
     */
    long size();

    /**
     * Associates the value with the specified key.
     *
     * @param key   the key to associated the value with
     * @param value the value to store
     */
    void put(K key, V value);

    /**
     * Associated the value with the specified key, only if no previous value hasn't been stored.
     *
     * @param key   the key to associated the value with
     * @param value the value to store
     */
    void putIfAbsent(K key, V value);

    /**
     * Associated the result of computation with the specified key, only if no previous value hasn't been stored.
     *
     * @param key      the key to associate the value with
     * @param supplier the supplier used for computing the value
     * @return the computed value or the the previous associated value
     */
    V computeIfAbsent(K key, Supplier<V> supplier);

    /**
     * Retrieves the value associated with the given key.
     *
     * @param key the key
     * @return the value associated with the given key, or null if no entry exists
     */
    V get(K key);

    /**
     * Retrieves the value associated with the given key.
     *
     * @param key the key
     * @return the value associated with the given key, or {@link Optional#empty()} if no entry exists
     */
    Optional<V> getOptional(K key);

    /**
     * Removes any entry associated with the specified key.
     *
     * @param key the key
     * @return the previously associated value
     */
    V remove(K key);

    /**
     * Replaces the previous value associated with given key and returns whether the operation succeeded.
     *
     * @param key      the key
     * @param oldValue the expected old value
     * @param newValue the value to replace with
     * @return whether the operation succeeded or not
     */
    boolean replace(K key, V oldValue, V newValue);

    /**
     * Invalidates the entry associated with the given key.
     *
     * @param key the key to invalidate
     * @param consumer the action to be performed on the key
     */
    void invalidateIfPresent(K key, Consumer<V> consumer);

    /**
     * Invalidates all entries in the cache.
     */
    void invalidateAll();
}
