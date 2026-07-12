package dev.breach.DistrictRP.database.tables;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.DataStore;
import dev.breach.DistrictRP.database.DatabaseTable;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class StaffPanelStaffDeptsTable extends DatabaseTable {

    public StaffPanelStaffDeptsTable(DistrictRP plugin, DataStore store) {
        super(plugin, store, "sp_staff_departments");
    }

    @Override
    protected String getCreateTableQuery() {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "uuid VARCHAR(36) NOT NULL, " +
                "username VARCHAR(64) NOT NULL, " +
                "department_id VARCHAR(64) NOT NULL, " +
                "PRIMARY KEY (uuid, department_id), " +
                "INDEX idx_dept (department_id)" +
                ") DEFAULT CHARSET=utf8mb4";
    }

    public CompletableFuture<Boolean> assign(UUID uuid, String username, String deptId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO " + tableName + " (uuid, username, department_id) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE username=VALUES(username)";
            try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
                st.setString(1, uuid.toString());
                st.setString(2, username);
                st.setString(3, deptId);
                st.executeUpdate();
                return true;
            } catch (SQLException e) { return false; }
        });
    }

    public CompletableFuture<Boolean> unassign(UUID uuid, String deptId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = getConnection();
                 PreparedStatement st = c.prepareStatement("DELETE FROM " + tableName + " WHERE uuid=? AND department_id=?")) {
                st.setString(1, uuid.toString());
                st.setString(2, deptId);
                return st.executeUpdate() > 0;
            } catch (SQLException e) { return false; }
        });
    }

    public CompletableFuture<List<Map<String, Object>>> listByDepartment(String deptId) {
        return CompletableFuture.supplyAsync(() -> {
            List<Map<String, Object>> out = new ArrayList<>();
            String sql = "SELECT uuid, username FROM " + tableName + " WHERE department_id=? ORDER BY username ASC";
            try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
                st.setString(1, deptId);
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("uuid", rs.getString("uuid"));
                        m.put("username", rs.getString("username"));
                        out.add(m);
                    }
                }
            } catch (SQLException ignored) {}
            return out;
        });
    }

    public CompletableFuture<Integer> countDistinctStaffers() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = getConnection();
                 PreparedStatement st = c.prepareStatement("SELECT COUNT(DISTINCT uuid) AS n FROM " + tableName);
                 ResultSet rs = st.executeQuery()) {
                if (rs.next()) return rs.getInt("n");
            } catch (SQLException ignored) {}
            return 0;
        });
    }
}