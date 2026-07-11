package dev.breach.DistrictRP.database.repository;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.roleplay.warp.Warp;
import dev.breach.DistrictRP.database.Repository;
import dev.breach.DistrictRP.database.tables.WarpsTable;
import dev.breach.DistrictRP.database.tables.WarpsTable.WarpRow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class WarpRepository implements Repository {

    private final DistrictRP plugin;
    private WarpsTable table;

    public WarpRepository(DistrictRP plugin) {
        this.plugin = plugin;
        if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isMariaDb()) {
            this.table = plugin.getDatabaseManager().getTable("warps", WarpsTable.class);
        }
    }

    @Override
    public boolean isAvailable() {
        return table != null;
    }

    public CompletableFuture<List<Warp>> loadAllWarps() {
        if (!isAvailable()) return CompletableFuture.completedFuture(new ArrayList<>());
        return table.all().thenApply(rows -> {
            List<Warp> out = new ArrayList<>();
            for (WarpRow r : rows) out.add(toWarp(r));
            return out;
        });
    }

    public CompletableFuture<Boolean> saveWarp(Warp w) {
        if (!isAvailable()) return CompletableFuture.completedFuture(false);
        return table.upsert(toRow(w));
    }

    public CompletableFuture<Boolean> deleteWarp(String name) {
        if (!isAvailable()) return CompletableFuture.completedFuture(false);
        return table.delete(name);
    }

    private Warp toWarp(WarpRow r) {
        return new Warp(r.name, r.world, r.x, r.y, r.z, r.yaw, r.pitch, r.permission);
    }

    private WarpRow toRow(Warp w) {
        WarpRow r = new WarpRow();
        r.name = w.getName();
        r.world = w.getWorld();
        r.x = w.getX();
        r.y = w.getY();
        r.z = w.getZ();
        r.yaw = w.getYaw();
        r.pitch = w.getPitch();
        r.permission = w.getPermission();
        return r;
    }
}