package dev.breach.DistrictRP.database.repository;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.Repository;
import dev.breach.DistrictRP.database.tables.ChatSymTable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ChatSymRepository implements Repository {

    private final DistrictRP plugin;
    private ChatSymTable table;

    public ChatSymRepository(DistrictRP plugin) {
        this.plugin = plugin;
        if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isMariaDb()) {
            this.table = plugin.getDatabaseManager().getTable("chatsym", ChatSymTable.class);
        }
    }

    @Override
    public boolean isAvailable() {
        return table != null;
    }

    public CompletableFuture<Boolean> upsert(String symbol, String command) {
        if (!isAvailable()) return CompletableFuture.completedFuture(false);
        return table.upsert(symbol, command);
    }

    public CompletableFuture<Boolean> delete(String symbol) {
        if (!isAvailable()) return CompletableFuture.completedFuture(false);
        return table.delete(symbol);
    }

    public CompletableFuture<Map<String, String>> fetchAll() {
        if (!isAvailable()) return CompletableFuture.completedFuture(new java.util.HashMap<>());
        return table.all();
    }
}