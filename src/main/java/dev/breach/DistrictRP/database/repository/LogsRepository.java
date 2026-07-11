package dev.breach.DistrictRP.database.repository;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.Repository;
import dev.breach.DistrictRP.database.tables.LogsTable;
import dev.breach.DistrictRP.database.tables.LogsTable.Row;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LogsRepository implements Repository {

    private final DistrictRP plugin;
    private LogsTable table;

    public LogsRepository(DistrictRP plugin) {
        this.plugin = plugin;
        if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isMariaDb()) {
            this.table = plugin.getDatabaseManager().getTable("logs", LogsTable.class);
        }
    }

    @Override
    public boolean isAvailable() {
        return table != null;
    }

    public CompletableFuture<Long> log(String module, UUID uuid, String name, String action) {
        if (!isAvailable()) return CompletableFuture.completedFuture(-1L);
        String serverId = plugin.getConfig().getString("server-id", Bukkit.getServer().getName());
        return table.add(module, uuid, name, action, serverId);
    }

    public CompletableFuture<List<Row>> query(String module, UUID uuid, int limit, int offset) {
        if (!isAvailable()) return CompletableFuture.completedFuture(new java.util.ArrayList<>());
        return table.query(module, uuid, limit, offset);
    }
}