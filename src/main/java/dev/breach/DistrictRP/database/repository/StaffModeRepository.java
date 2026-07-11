package dev.breach.DistrictRP.database.repository;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.Repository;
import dev.breach.DistrictRP.database.tables.StaffModeTable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class StaffModeRepository implements Repository {

    private final DistrictRP plugin;
    private StaffModeTable table;

    public StaffModeRepository(DistrictRP plugin) {
        this.plugin = plugin;
        if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isMariaDb()) {
            this.table = plugin.getDatabaseManager().getTable("staffmode", StaffModeTable.class);
        }
    }

    @Override
    public boolean isAvailable() {
        return table != null;
    }

    public CompletableFuture<Boolean> setActive(UUID uuid, boolean active, String snapshotJson) {
        if (!isAvailable()) return CompletableFuture.completedFuture(false);
        return table.setActive(uuid, active, snapshotJson);
    }

    public CompletableFuture<String> getSnapshot(UUID uuid) {
        if (!isAvailable()) return CompletableFuture.completedFuture(null);
        return table.getSnapshot(uuid);
    }

    public CompletableFuture<Boolean> isActive(UUID uuid) {
        if (!isAvailable()) return CompletableFuture.completedFuture(false);
        return table.isActive(uuid);
    }
}