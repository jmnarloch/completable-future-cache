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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

/**
 * Caches the {@link CompletableFuture} computations and their results. Typical use case requires to associated
 * a supplier with a specific key which will trigger a task being scheduled for the execution to the
 * {@link CompletableFuture}, until the task will be completed any sequential calls to {@link #supply(Object, Supplier)}
 * will return the same {@link CompletableFuture} that can be observed for completion. Once the task will be completed
 * the result will be cached. Any subsequent call will be returning the value being wrapped in
 * {@link CompletableFuture#completedFuture(Object)}.
 *
 * A {@link CompletableFuture} that will end processing with an exception will be removed from cache.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @author Jakub Narloch
 * @see CompletableFuture
 */
public interface CompletableFutureCache<K, V> {

    /**
     * Returns whether the cache does not contains any entries.
     *
     * @return true if cache is empty
     */
    boolean isEmpty();

    /**
     * Returns the number of entries in the cache.
     *
     * @return the number of entries
     */
    long size();

    /**
     * Associated the specific supplier with the key. In case that no previous task has been stored, invoking this
     * operation will schedule a task for execution and return it as {@link CompletableFuture}. In case that the
     * {@link CompletableFuture} hasn't been completed, any subsequent calls will return the same future.
     *
     * @param key      the key to associate the specific supplier with
     * @param supplier the supplier to be executed
     * @return the future
     * @throws NullPointerException if {@code key} is {@code null}
     *                              or {@code supplier} is {@code null}
     * @throws CompletionException  if an error occurs when scheduling the {@code supplier} for execution
     */
    CompletableFuture<V> supply(K key, Supplier<V> supplier);

    /**
     * Returns the {@link CompletableFuture} associated with the given key.
     *
     * @param key the key
     * @return the value associated with the specified key, null if no entry exists
     * @throws NullPointerException if {@code key} is {@code null}
     */
    CompletableFuture<V> get(K key);

    /**
     * Returns the optional {@link CompletableFuture} associated with the given key.
     *
     * @param key the key
     * @return the value associated with the specified key, {@link Optional#empty()} if no entry exists
     * @throws NullPointerException if {@code key} is {@code null}
     */
    Optional<CompletableFuture<V>> getOptional(K key);

    /**
     * Invalidates the {@link CompletableFuture} associated with the given key.
     *
     * @param key the key to invalidate
     * @throws NullPointerException if {@code key} is {@code null}
     */
    void invalidate(K key);

    /**
     * Invalidates all entries in the cache.
     */
    void invalidateAll();
}
