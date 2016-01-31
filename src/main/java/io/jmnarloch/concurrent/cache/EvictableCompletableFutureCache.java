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

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * A {@link CompletableFutureCache} that evicts it's entries after configurable amount of time.
 *
 * Typical use case requires to associated
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
 * @see CompletableFutureCache
 */
public final class EvictableCompletableFutureCache<K, V> implements CompletableFutureCache<K, V> {

    private final Cache<K, CompletableFuture<V>> cache;
    private final CompletableFutureExecutor executor;

    /**
     * Creates new instance of {@link EvictableCompletableFutureCache} with executor and specified duration before the
     * entries will be evicted.
     *
     * @param executor the executor to schedule the task
     * @param duration the duration after which the entries will be evicted
     * @param unit     the time unit
     */
    public EvictableCompletableFutureCache(Executor executor, long duration, TimeUnit unit) {
        this.cache = new EvictableCache<>(duration, unit);
        this.executor = new CompletableFutureExecutor(executor);
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
    public CompletableFuture<V> supply(K key, Supplier<V> supplier) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(supplier);

        return cache.computeIfAbsent(key, () ->
                executor.supplyAsync(supplier, new CompletableFutureObserver(key))
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<V> get(K key) {
        Objects.requireNonNull(key);

        return cache.get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<CompletableFuture<V>> getOptional(K key) {
        return Optional.ofNullable(get(key));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invalidate(K key) {
        Objects.requireNonNull(key);

        cache.invalidateIfPresent(key, (v) -> v.cancel(true));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invalidateAll() {
        cache.invalidateAll();
    }

    private void put(K key, CompletableFuture<V> value) {
        cache.put(key, value);
    }

    private void remove(K key) {
        cache.remove(key);
    }

    private CompletableFuture<V> cached(V value) {
        return CompletableFuture.completedFuture(value);
    }

    private static final class CompletableFutureExecutor {

        private final Executor executor;

        CompletableFutureExecutor(Executor executor) {
            this.executor = executor;
        }

        <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier, BiConsumer<T, Throwable> observer) {
            return observe(supplyAsync(supplier), observer);
        }

        private <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
            return CompletableFuture.supplyAsync(supplier, executor);
        }

        private <T> CompletableFuture<T> observe(CompletableFuture<T> observable, BiConsumer<T, Throwable> observer) {
            return observable.whenComplete(observer);
        }
    }

    private class CompletableFutureObserver implements BiConsumer<V, Throwable> {

        private final K key;

        public CompletableFutureObserver(K key) {
            this.key = key;
        }

        @Override
        public void accept(V value, Throwable throwable) {
            if (throwable != null) {
                remove(key);
            } else {
                final CompletableFuture<V> cached = cached(value);
                put(key, cached);
            }
        }

    }
}
