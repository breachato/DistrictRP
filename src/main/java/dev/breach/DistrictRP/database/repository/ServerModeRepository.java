package dev.breach.DistrictRP.database.repository;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.Repository;
import dev.breach.DistrictRP.database.tables.ServerModeTable;

import java.util.concurrent.CompletableFuture;

public class ServerModeRepository implements Repository {

    private final DistrictRP plugin;
    private ServerModeTable table;

    public ServerModeRepository(DistrictRP plugin) {
        this.plugin = plugin;
        if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isMariaDb()) {
            this.table = plugin.getDatabaseManager().getTable("server_mode", ServerModeTable.class);
        }
    }

    @Override
    public boolean isAvailable() {
        return table != null;
    }

    public CompletableFuture<Boolean> setMode(String serverId, String mode, String by) {
        if (!isAvailable()) return CompletableFuture.completedFuture(false);
        return table.set(serverId, mode, by);
    }

    public CompletableFuture<String> getMode(String serverId) {
        if (!isAvailable()) return CompletableFuture.completedFuture("OFF");
        return table.get(serverId);
    }
}