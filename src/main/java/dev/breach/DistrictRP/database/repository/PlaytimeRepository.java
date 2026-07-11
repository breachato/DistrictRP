package dev.breach.DistrictRP.database.repository;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.roleplay.playtime.PlaytimeData;
import dev.breach.DistrictRP.database.Repository;
import dev.breach.DistrictRP.database.tables.PlaytimeTable;
import dev.breach.DistrictRP.database.tables.PlaytimeTable.PlaytimeRow;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlaytimeRepository implements Repository {

    private final DistrictRP plugin;
    private PlaytimeTable table;

    public PlaytimeRepository(DistrictRP plugin) {
        this.plugin = plugin;
        if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isMariaDb()) {
            this.table = plugin.getDatabaseManager().getTable("playtime", PlaytimeTable.class);
        }
    }

    @Override
    public boolean isAvailable() {
        return table != null;
    }

    public CompletableFuture<PlaytimeData> load(UUID uuid) {
        if (!isAvailable()) return CompletableFuture.completedFuture(null);
        return table.get(uuid).thenApply(row -> row == null ? null : toData(row));
    }

    public CompletableFuture<Boolean> save(UUID uuid, PlaytimeData data) {
        if (!isAvailable()) return CompletableFuture.completedFuture(false);
        return table.upsert(toRow(uuid, data));
    }

    private PlaytimeData toData(PlaytimeRow r) {
        PlaytimeData d = new PlaytimeData();
        d.setTotalSeconds(r.total);
        d.setDailySeconds(r.daily);
        d.setWeeklySeconds(r.weekly);
        d.setMonthlySeconds(r.monthly);
        d.setDailyReset(r.lastResetDaily);
        d.setWeeklyReset(r.lastResetWeekly);
        d.setMonthlyReset(r.lastResetMonthly);
        return d;
    }

    private PlaytimeRow toRow(UUID uuid, PlaytimeData d) {
        PlaytimeRow r = new PlaytimeRow();
        r.uuid = uuid;
        r.total = d.getTotalSeconds();
        r.daily = d.getDailySeconds();
        r.weekly = d.getWeeklySeconds();
        r.monthly = d.getMonthlySeconds();
        r.lastResetDaily = d.getDailyReset();
        r.lastResetWeekly = d.getWeeklyReset();
        r.lastResetMonthly = d.getMonthlyReset();
        return r;
    }
}