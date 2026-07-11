package dev.breach.DistrictRP.database.tables;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.DataStore;
import dev.breach.DistrictRP.database.DatabaseTable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class WarpsTable extends DatabaseTable {

    public WarpsTable(DistrictRP plugin, DataStore store) { super(plugin, store, "warps"); }

    @Override
    protected String getCreateTableQuery() {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "name VARCHAR(64) PRIMARY KEY," +
                "world VARCHAR(64) NOT NULL," +
                "x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL," +
                "yaw FLOAT NOT NULL DEFAULT 0, pitch FLOAT NOT NULL DEFAULT 0," +
                "permission VARCHAR(128) DEFAULT ''," +
                "created_at BIGINT NOT NULL" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
    }

    public CompletableFuture<Boolean> upsert(WarpRow r) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO " + tableName +
                    " (name, world, x, y, z, yaw, pitch, permission, created_at) VALUES (?,?,?,?,?,?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE world=VALUES(world), x=VALUES(x), y=VALUES(y), z=VALUES(z), " +
                    "yaw=VALUES(yaw), pitch=VALUES(pitch), permission=VALUES(permission)";
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, r.name.toLowerCase());
                ps.setString(2, r.world);
                ps.setDouble(3, r.x); ps.setDouble(4, r.y); ps.setDouble(5, r.z);
                ps.setFloat(6, r.yaw); ps.setFloat(7, r.pitch);
                ps.setString(8, r.permission == null ? "" : r.permission);
                ps.setLong(9, System.currentTimeMillis());
                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB warps.upsert] " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> delete(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = getConnection();
                 PreparedStatement ps = c.prepareStatement("DELETE FROM " + tableName + " WHERE name=?")) {
                ps.setString(1, name.toLowerCase());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) { return false; }
        });
    }

    public CompletableFuture<List<WarpRow>> all() {
        return CompletableFuture.supplyAsync(() -> {
            List<WarpRow> out = new ArrayList<>();
            try (Connection c = getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM " + tableName)) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        WarpRow r = new WarpRow();
                        r.name = rs.getString("name");
                        r.world = rs.getString("world");
                        r.x = rs.getDouble("x"); r.y = rs.getDouble("y"); r.z = rs.getDouble("z");
                        r.yaw = rs.getFloat("yaw"); r.pitch = rs.getFloat("pitch");
                        r.permission = rs.getString("permission");
                        r.createdAt = rs.getLong("created_at");
                        out.add(r);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB warps.all] " + e.getMessage());
            }
            return out;
        });
    }

    public static class WarpRow {
        public String name, world, permission;
        public double x, y, z;
        public float yaw, pitch;
        public long createdAt;
    }
}