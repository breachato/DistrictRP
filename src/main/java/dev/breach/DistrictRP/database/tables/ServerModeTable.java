package dev.breach.DistrictRP.database.tables;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.DataStore;
import dev.breach.DistrictRP.database.DatabaseTable;

import java.sql.*;
import java.util.concurrent.CompletableFuture;

public class ServerModeTable extends DatabaseTable {

    public ServerModeTable(DistrictRP plugin, DataStore store) { super(plugin, store, "server_mode"); }

    @Override
    protected String getCreateTableQuery() {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "server_id VARCHAR(64) PRIMARY KEY," +
                "mode VARCHAR(32) NOT NULL DEFAULT 'OFF'," +
                "updated_at BIGINT NOT NULL," +
                "updated_by VARCHAR(32) DEFAULT NULL" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
    }

    public CompletableFuture<Boolean> set(String serverId, String mode, String by) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO " + tableName + " (server_id, mode, updated_at, updated_by) VALUES (?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE mode=VALUES(mode), updated_at=VALUES(updated_at), updated_by=VALUES(updated_by)";
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, serverId);
                ps.setString(2, mode);
                ps.setLong(3, System.currentTimeMillis());
                ps.setString(4, by);
                ps.executeUpdate();
                return true;
            } catch (SQLException e) { return false; }
        });
    }

    public CompletableFuture<String> get(String serverId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT mode FROM " + tableName + " WHERE server_id=?")) {
                ps.setString(1, serverId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getString(1);
                }
            } catch (SQLException ignored) {}
            return "OFF";
        });
    }
}