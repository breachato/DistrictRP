package dev.breach.DistrictRP.database.tables;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.DataStore;
import dev.breach.DistrictRP.database.DatabaseTable;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlaytimeTable extends DatabaseTable {

    public PlaytimeTable(DistrictRP plugin, DataStore store) { super(plugin, store, "playtime"); }

    @Override
    protected String getCreateTableQuery() {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "total_seconds BIGINT DEFAULT 0," +
                "daily_seconds BIGINT DEFAULT 0," +
                "weekly_seconds BIGINT DEFAULT 0," +
                "monthly_seconds BIGINT DEFAULT 0," +
                "last_reset_daily BIGINT DEFAULT 0," +
                "last_reset_weekly BIGINT DEFAULT 0," +
                "last_reset_monthly BIGINT DEFAULT 0," +
                "updated_at BIGINT NOT NULL" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
    }

    public CompletableFuture<Boolean> upsert(PlaytimeRow r) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO " + tableName +
                    " (uuid, total_seconds, daily_seconds, weekly_seconds, monthly_seconds, " +
                    "last_reset_daily, last_reset_weekly, last_reset_monthly, updated_at) VALUES (?,?,?,?,?,?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE total_seconds=VALUES(total_seconds), daily_seconds=VALUES(daily_seconds), " +
                    "weekly_seconds=VALUES(weekly_seconds), monthly_seconds=VALUES(monthly_seconds), " +
                    "last_reset_daily=VALUES(last_reset_daily), last_reset_weekly=VALUES(last_reset_weekly), " +
                    "last_reset_monthly=VALUES(last_reset_monthly), updated_at=VALUES(updated_at)";
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, r.uuid.toString());
                ps.setLong(2, r.total); ps.setLong(3, r.daily);
                ps.setLong(4, r.weekly); ps.setLong(5, r.monthly);
                ps.setLong(6, r.lastResetDaily); ps.setLong(7, r.lastResetWeekly);
                ps.setLong(8, r.lastResetMonthly);
                ps.setLong(9, System.currentTimeMillis());
                ps.executeUpdate();
                return true;
            } catch (SQLException e) { return false; }
        });
    }

    public CompletableFuture<PlaytimeRow> get(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM " + tableName + " WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    PlaytimeRow r = new PlaytimeRow();
                    r.uuid = UUID.fromString(rs.getString("uuid"));
                    r.total = rs.getLong("total_seconds");
                    r.daily = rs.getLong("daily_seconds");
                    r.weekly = rs.getLong("weekly_seconds");
                    r.monthly = rs.getLong("monthly_seconds");
                    r.lastResetDaily = rs.getLong("last_reset_daily");
                    r.lastResetWeekly = rs.getLong("last_reset_weekly");
                    r.lastResetMonthly = rs.getLong("last_reset_monthly");
                    return r;
                }
            } catch (SQLException e) { return null; }
        });
    }

    public static class PlaytimeRow {
        public UUID uuid;
        public long total, daily, weekly, monthly;
        public long lastResetDaily, lastResetWeekly, lastResetMonthly;
    }
}