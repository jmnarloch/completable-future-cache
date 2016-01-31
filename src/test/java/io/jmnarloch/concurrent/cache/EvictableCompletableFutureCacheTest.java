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

import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link EvictableCompletableFutureCache} class.
 *
 * @author Jakub Narloch
 */
public class EvictableCompletableFutureCacheTest {

    private Executor executor;

    private CompletableFutureCache<String, String> instance;

    @Before
    public void setUp() throws Exception {

        executor = Executors.newFixedThreadPool(5);
        instance = new EvictableCompletableFutureCache<>(executor, 10, TimeUnit.SECONDS);
    }

    @Test
    public void shouldBeEmpty() {

        // expect
        assertTrue(instance.isEmpty());
    }

    @Test
    public void shouldNotBeEmpty() {

        // given
        final String key = "task";
        final Supplier<String> supplier = () -> "completed";

        // when
        instance.supply(key, supplier);

        // then
        assertFalse(instance.isEmpty());
    }

    @Test
    public void shouldHaveZeroSizeForEmpty() {

        // expect
        assertEquals(0, instance.size());
    }

    @Test
    public void shouldHaveNonZeroSizeForNonEmpty() {

        // given
        final String key = "task";
        final Supplier<String> supplier = () -> "completed";

        // when
        instance.supply(key, supplier);

        // then
        assertEquals(1, instance.size());
    }

    @Test(expected = NullPointerException.class)
    public void shouldSupplyRejectNullKey() {

        // given
        final Supplier<String> supplier = () -> "completed";

        // when
        instance.supply(null, supplier);
    }

    @Test(expected = NullPointerException.class)
    public void shouldSupplyRejectNullSupplier() {

        // given
        final String key = "task";

        // when
        instance.supply(key, null);
    }

    @Test
    public void shouldSupplyTaskAndAwaitCompletion() {

        // given
        final String key = "task";
        final Supplier<String> supplier = () -> "completed";

        // when
        final CompletableFuture<String> future = instance.supply(key, supplier);

        // then
        assertEquals(supplier.get(), future.join());
    }

    @Test
    public void shouldSupplyTaskAndRetrievedCachedValue() throws InterruptedException {

        // given
        final String key = "task";
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final Supplier<String> supplier = () -> {
            countDownLatch.countDown();
            return "completed";
        };
        final CompletableFuture<String> future = instance.supply(key, supplier);
        countDownLatch.await();

        // when
        CompletableFuture<String> cached;
        do {
            cached = instance.supply(key, supplier);
        } while (future == cached);

        // then
        assertNotSame(future, cached);
        assertTrue(cached.isDone());
        assertEquals(supplier.get(), cached.join());
    }

    @Test
    public void shouldSupplyTaskAndMaintainSingleTask() throws InterruptedException {

        // given
        final int iter = 100;
        final String key = "task";
        final CountDownLatch countDownLatch = new CountDownLatch(iter);
        final Supplier<String> supplier = () -> {
            try {
                countDownLatch.await();
                return "completed";
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };

        // and
        final CompletableFuture<String> future = instance.supply(key, supplier);

        // expect
        for (int ind = 0; ind < iter; ind++) {
            final CompletableFuture<String> cached = instance.supply(key, supplier);
            assertSame(future, cached);
            assertTrue(!cached.isDone());
            countDownLatch.countDown();
        }
    }

    @Test
    public void shouldRemoveFutureOnError() {

        // given
        final String key = "task";
        final Supplier<String> supplier = () -> {
            throw new RuntimeException("unexpected");
        };

        // when
        final CompletableFuture<String> future = instance.supply(key, supplier);

        // then
        try {
            future.join();
            fail();
        } catch (CompletionException e) {
            assertTrue(future.isCompletedExceptionally());
            assertNull(instance.get(key));
        }
    }

    @Test(expected = NullPointerException.class)
    public void shouldGetRejectNullKey() {

        // when
        instance.get(null);
    }

    @Test
    public void shouldGetFuture() {

        // given
        final String key = "task";
        final Supplier<String> supplier = () -> "completed";
        instance.supply(key, supplier);

        // when
        final CompletableFuture<String> future = instance.get(key);

        // then
        assertNotNull(future);
        assertEquals(supplier.get(), future.join());
    }

    @Test(expected = NullPointerException.class)
    public void shouldGetOptionalRejectNullKey() {

        // when
        instance.getOptional(null);
    }

    @Test
    public void shouldGetOptionalFuture() {

        // given
        final String key = "task";
        final Supplier<String> supplier = () -> "completed";
        instance.supply(key, supplier);

        // when
        final Optional<CompletableFuture<String>> future = instance.getOptional(key);

        // then
        assertNotNull(future);
        assertNotNull(future.get());
        assertEquals(supplier.get(), future.get().join());
    }

    @Test(expected = NullPointerException.class)
    public void shouldInvalidateRejectNullKey() {

        // when
        instance.invalidate(null);
    }

    @Test
    public void shouldInvalidateTask() {

        // given
        final String key = "task";
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final Supplier<String> supplier = () -> {
            try {
                countDownLatch.await();
                return "completed";
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        final CompletableFuture<String> future = instance.supply(key, supplier);

        // when
        instance.invalidate(key);

        // then
        countDownLatch.countDown();
        assertTrue(future.isCancelled());
    }

    @Test
    public void shouldInvalidateAllTasks() {

        // given
        final int iter = 5;
        final String key = "task";
        final Supplier<String> supplier = () -> "completed";

        // and
        for(int ind = 0; ind < iter; ind++) {
            instance.supply(String.format("%s_%d", key, iter), supplier);
        }

        // when
        instance.invalidateAll();

        // then
        assertEquals(0, instance.size());
    }
}