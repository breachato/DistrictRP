package dev.breach.DistrictRP.database.tables;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.DataStore;
import dev.breach.DistrictRP.database.DatabaseTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class StaffModeTable extends DatabaseTable {

    public StaffModeTable(DistrictRP plugin, DataStore store) {
        super(plugin, store, "staffmode");
    }

    @Override
    protected String getCreateTableQuery() {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "active BOOLEAN NOT NULL DEFAULT FALSE," +
                "snapshot_json LONGTEXT DEFAULT NULL," +
                "updated_at BIGINT NOT NULL" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
    }

    public CompletableFuture<Boolean> setActive(UUID uuid, boolean active, String snapshotJson) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO " + tableName + " (uuid, active, snapshot_json, updated_at) VALUES (?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE active=VALUES(active), snapshot_json=VALUES(snapshot_json), updated_at=VALUES(updated_at)";
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setBoolean(2, active);
                ps.setString(3, snapshotJson);
                ps.setLong(4, System.currentTimeMillis());
                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB staffmode.setActive] " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<String> getSnapshot(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT snapshot_json FROM " + tableName + " WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getString(1);
                }
            } catch (SQLException ignored) {}
            return null;
        });
    }

    public CompletableFuture<Boolean> isActive(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT active FROM " + tableName + " WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getBoolean(1);
                }
            } catch (SQLException ignored) {}
            return false;
        });
    }
}