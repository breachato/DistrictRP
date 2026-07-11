package dev.breach.DistrictRP.database.tables;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.DataStore;
import dev.breach.DistrictRP.database.DatabaseTable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VanishTable extends DatabaseTable {

    public VanishTable(DistrictRP plugin, DataStore store) { super(plugin, store, "vanish"); }

    @Override
    protected String getCreateTableQuery() {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "vanished BOOLEAN NOT NULL DEFAULT FALSE," +
                "updated_at BIGINT NOT NULL" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
    }

    public CompletableFuture<Boolean> set(UUID uuid, boolean v) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO " + tableName + " (uuid, vanished, updated_at) VALUES (?,?,?) " +
                    "ON DUPLICATE KEY UPDATE vanished=VALUES(vanished), updated_at=VALUES(updated_at)";
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setBoolean(2, v);
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
                return true;
            } catch (SQLException e) { return false; }
        });
    }

    public CompletableFuture<List<UUID>> allVanished() {
        return CompletableFuture.supplyAsync(() -> {
            List<UUID> out = new ArrayList<>();
            try (Connection c = getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT uuid FROM " + tableName + " WHERE vanished=TRUE")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(UUID.fromString(rs.getString(1)));
                }
            } catch (SQLException ignored) {}
            return out;
        });
    }
}