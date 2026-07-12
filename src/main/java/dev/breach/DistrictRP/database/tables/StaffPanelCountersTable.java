package dev.breach.DistrictRP.database.tables;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.DataStore;
import dev.breach.DistrictRP.database.DatabaseTable;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class StaffPanelCountersTable extends DatabaseTable {

    public StaffPanelCountersTable(DistrictRP plugin, DataStore store) {
        super(plugin, store, "sp_staff_counters");
    }

    @Override
    protected String getCreateTableQuery() {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "uuid VARCHAR(36) NOT NULL, " +
                "department_id VARCHAR(64) NOT NULL, " +
                "column_key VARCHAR(64) NOT NULL, " +
                "value INT NOT NULL DEFAULT 0, " +
                "updated_at BIGINT DEFAULT 0, " +
                "PRIMARY KEY (uuid, department_id, column_key), " +
                "INDEX idx_dept (department_id), " +
                "INDEX idx_dept_col (department_id, column_key)" +
                ") DEFAULT CHARSET=utf8mb4";
    }

    public CompletableFuture<Integer> setValue(UUID uuid, String deptId, String col, int value) {
        return CompletableFuture.supplyAsync(() -> {
            int v = Math.max(0, value);
            String sql = "INSERT INTO " + tableName + " (uuid, department_id, column_key, value, updated_at) VALUES (?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE value=VALUES(value), updated_at=VALUES(updated_at)";
            try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
                st.setString(1, uuid.toString());
                st.setString(2, deptId);
                st.setString(3, col);
                st.setInt(4, v);
                st.setLong(5, System.currentTimeMillis());
                st.executeUpdate();
                return v;
            } catch (SQLException e) { return -1; }
        });
    }

    public CompletableFuture<Integer> increment(UUID uuid, String deptId, String col, int delta) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = getConnection()) {
                c.setAutoCommit(false);
                try (PreparedStatement sel = c.prepareStatement(
                        "SELECT value FROM " + tableName + " WHERE uuid=? AND department_id=? AND column_key=? FOR UPDATE")) {
                    sel.setString(1, uuid.toString());
                    sel.setString(2, deptId);
                    sel.setString(3, col);
                    int current = 0;
                    try (ResultSet rs = sel.executeQuery()) {
                        if (rs.next()) current = rs.getInt("value");
                    }
                    int next = Math.max(0, current + delta);
                    try (PreparedStatement up = c.prepareStatement(
                            "INSERT INTO " + tableName + " (uuid, department_id, column_key, value, updated_at) VALUES (?, ?, ?, ?, ?) " +
                                    "ON DUPLICATE KEY UPDATE value=VALUES(value), updated_at=VALUES(updated_at)")) {
                        up.setString(1, uuid.toString());
                        up.setString(2, deptId);
                        up.setString(3, col);
                        up.setInt(4, next);
                        up.setLong(5, System.currentTimeMillis());
                        up.executeUpdate();
                    }
                    c.commit();
                    return next;
                } catch (SQLException e) {
                    c.rollback();
                    throw e;
                }
            } catch (SQLException e) { return -1; }
        });
    }

    public CompletableFuture<Map<String, Integer>> valuesFor(UUID uuid, String deptId) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Integer> out = new LinkedHashMap<>();
            String sql = "SELECT column_key, value FROM " + tableName + " WHERE uuid=? AND department_id=?";
            try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
                st.setString(1, uuid.toString());
                st.setString(2, deptId);
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) out.put(rs.getString("column_key"), rs.getInt("value"));
                }
            } catch (SQLException ignored) {}
            return out;
        });
    }

    public CompletableFuture<Integer> sumForDepartment(String deptId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = getConnection();
                 PreparedStatement st = c.prepareStatement("SELECT COALESCE(SUM(value),0) AS s FROM " + tableName + " WHERE department_id=?")) {
                st.setString(1, deptId);
                try (ResultSet rs = st.executeQuery()) {
                    if (rs.next()) return rs.getInt("s");
                }
            } catch (SQLException ignored) {}
            return 0;
        });
    }

    public CompletableFuture<Integer> sumAll() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = getConnection();
                 PreparedStatement st = c.prepareStatement("SELECT COALESCE(SUM(value),0) AS s FROM " + tableName);
                 ResultSet rs = st.executeQuery()) {
                if (rs.next()) return rs.getInt("s");
            } catch (SQLException ignored) {}
            return 0;
        });
    }
}