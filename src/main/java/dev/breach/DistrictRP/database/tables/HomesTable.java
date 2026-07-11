package dev.breach.DistrictRP.database.tables;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.DataStore;
import dev.breach.DistrictRP.database.DatabaseTable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class HomesTable extends DatabaseTable {

    public HomesTable(DistrictRP plugin, DataStore store) { super(plugin, store, "homes"); }

    @Override
    protected String getCreateTableQuery() {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "uuid VARCHAR(36) NOT NULL," +
                "name VARCHAR(32) NOT NULL," +
                "world VARCHAR(64) NOT NULL," +
                "x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL," +
                "yaw FLOAT NOT NULL DEFAULT 0, pitch FLOAT NOT NULL DEFAULT 0," +
                "created_at BIGINT NOT NULL," +
                "PRIMARY KEY (uuid, name)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
    }

    public CompletableFuture<Boolean> upsert(HomeRow r) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO " + tableName + " (uuid, name, world, x, y, z, yaw, pitch, created_at) VALUES (?,?,?,?,?,?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE world=VALUES(world), x=VALUES(x), y=VALUES(y), z=VALUES(z), yaw=VALUES(yaw), pitch=VALUES(pitch)";
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, r.uuid.toString());
                ps.setString(2, r.name.toLowerCase());
                ps.setString(3, r.world);
                ps.setDouble(4, r.x); ps.setDouble(5, r.y); ps.setDouble(6, r.z);
                ps.setFloat(7, r.yaw); ps.setFloat(8, r.pitch);
                ps.setLong(9, System.currentTimeMillis());
                ps.executeUpdate();
                return true;
            } catch (SQLException e) { return false; }
        });
    }

    public CompletableFuture<Boolean> delete(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = getConnection();
                 PreparedStatement ps = c.prepareStatement("DELETE FROM " + tableName + " WHERE uuid=? AND name=?")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name.toLowerCase());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) { return false; }
        });
    }

    public CompletableFuture<List<HomeRow>> listByOwner(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<HomeRow> out = new ArrayList<>();
            try (Connection c = getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM " + tableName + " WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        HomeRow r = new HomeRow();
                        r.uuid = UUID.fromString(rs.getString("uuid"));
                        r.name = rs.getString("name");
                        r.world = rs.getString("world");
                        r.x = rs.getDouble("x"); r.y = rs.getDouble("y"); r.z = rs.getDouble("z");
                        r.yaw = rs.getFloat("yaw"); r.pitch = rs.getFloat("pitch");
                        r.createdAt = rs.getLong("created_at");
                        out.add(r);
                    }
                }
            } catch (SQLException ignored) {}
            return out;
        });
    }

    public static class HomeRow {
        public UUID uuid;
        public String name, world;
        public double x, y, z;
        public float yaw, pitch;
        public long createdAt;
    }
}