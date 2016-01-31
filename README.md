# CompletableFuture cache

> Caches CompletableFuture computations and their results

[![Build Status](https://travis-ci.org/jmnarloch/completable-future-cache.svg?branch=master)](https://travis-ci.org/jmnarloch/completable-future-cache)
[![Coverage Status](https://coveralls.io/repos/jmnarloch/completable-future-cache/badge.svg?branch=master&service=github)](https://coveralls.io/github/jmnarloch/completable-future-cache?branch=master)

## Setup

Add the module to your project:

```xml
<dependency>
  <groupId>io.jmnarloch</groupId>
  <artifactId>completable-future-cache</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Features

A specialized cache for storing the CompletableFuture computations and caching their results.

```
ExecutorService executor = Executors.newFixedThreadPool(20);
EvictableCompletableFutureCache<String, User> cache = new EvictableCompletableFutureCache<>(executor, 10, TimeUnit.SECONDS);
```

Typical usage scenario requires to supply a unit of work to the cache through `supply` method,
this will trigger the execution of the task and return it in a form of `CompletableFuture`.

```
public class MongoUserRepository {

    private final MongoOperations mongoOperations;

    private final CompletableFutureCache<String, User> userCache;

    ...

    public CompletableFuture<User> findByEmail(String email) {

        return userCache.supply(email, () -> queryByEmail(email));
    }
}
```

The cache is going to observe the future for completion. Once the future finishes processing the task, the result will
be wrapped into `CompletableFuture` through `CompletableFuture#completedFuture` and cached.

Note: It's worth noticing that due to the asynchronous nature of the future the cache can not block and wait for the
value to be computed. The cache guarantees that as long as the value did not expired at most one task will
be processing the supplied unit of work, effectively using your resources.

The consequences is that when multiple supply methods are going to consecutively invoked all of them will gain access to
the *same* instance of `CompletableFuture` that they can observe for completion.

## License

Apache 2.0