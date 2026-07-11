package dev.breach.DistrictRP.database;

import java.util.concurrent.CompletableFuture;

public interface Repository {

    boolean isAvailable();

    default CompletableFuture<Void> flushAll() {
        return CompletableFuture.completedFuture(null);
    }

    default CompletableFuture<Void> loadAll() {
        return CompletableFuture.completedFuture(null);
    }
}