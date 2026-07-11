package dev.breach.DistrictRP.database.repository;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.Repository;
import dev.breach.DistrictRP.database.tables.VanishTable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VanishRepository implements Repository {

    private final DistrictRP plugin;
    private VanishTable table;

    public VanishRepository(DistrictRP plugin) {
        this.plugin = plugin;
        if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isMariaDb()) {
            this.table = plugin.getDatabaseManager().getTable("vanish", VanishTable.class);
        }
    }

    @Override
    public boolean isAvailable() {
        return table != null;
    }

    public CompletableFuture<Boolean> setVanished(UUID uuid, boolean v) {
        if (!isAvailable()) return CompletableFuture.completedFuture(false);
        return table.set(uuid, v);
    }

    public CompletableFuture<List<UUID>> loadAllVanished() {
        if (!isAvailable()) return CompletableFuture.completedFuture(new java.util.ArrayList<>());
        return table.allVanished();
    }
}