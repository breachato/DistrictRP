package dev.breach.DistrictRP.database.tables;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.DataStore;
import dev.breach.DistrictRP.database.DatabaseTable;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class StaffPanelDeptColumnsTable extends DatabaseTable {

    public StaffPanelDeptColumnsTable(DistrictRP plugin, DataStore store) {
        super(plugin, store, "sp_department_columns");
    }

    @Override
    protected String getCreateTableQuery() {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "department_id VARCHAR(64) NOT NULL, " +
                "column_key VARCHAR(64) NOT NULL, " +
                "position INT DEFAULT 0, " +
                "PRIMARY KEY (department_id, column_key)" +
                ") DEFAULT CHARSET=utf8mb4";
    }

    public CompletableFuture<Boolean> add(String deptId, String columnKey, int position) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT IGNORE INTO " + tableName + " (department_id, column_key, position) VALUES (?, ?, ?)";
            try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
                st.setString(1, deptId);
                st.setString(2, columnKey);
                st.setInt(3, position);
                return st.executeUpdate() > 0;
            } catch (SQLException e) { return false; }
        });
    }

    public CompletableFuture<Boolean> remove(String deptId, String columnKey) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = getConnection();
                 PreparedStatement st = c.prepareStatement("DELETE FROM " + tableName + " WHERE department_id=? AND column_key=?")) {
                st.setString(1, deptId);
                st.setString(2, columnKey);
                return st.executeUpdate() > 0;
            } catch (SQLException e) { return false; }
        });
    }

    public CompletableFuture<List<String>> listFor(String deptId) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> out = new ArrayList<>();
            String sql = "SELECT column_key FROM " + tableName + " WHERE department_id=? ORDER BY position ASC, column_key ASC";
            try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
                st.setString(1, deptId);
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) out.add(rs.getString("column_key"));
                }
            } catch (SQLException ignored) {}
            return out;
        });
    }
}