package dev.breach.DistrictRP.database.tables;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.DataStore;
import dev.breach.DistrictRP.database.DatabaseTable;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class StaffPanelFlagsTable extends DatabaseTable {

    public StaffPanelFlagsTable(DistrictRP plugin, DataStore store) {
        super(plugin, store, "sp_staff_flags");
    }

    @Override
    protected String getCreateTableQuery() {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "uuid VARCHAR(36) NOT NULL, " +
                "department_id VARCHAR(64) NOT NULL, " +
                "flag VARCHAR(32) NOT NULL, " +
                "active TINYINT(1) NOT NULL DEFAULT 0, " +
                "note VARCHAR(255), " +
                "updated_at BIGINT DEFAULT 0, " +
                "PRIMARY KEY (uuid, department_id, flag)" +
                ") DEFAULT CHARSET=utf8mb4";
    }

    public CompletableFuture<Boolean> setFlag(UUID uuid, String deptId, String flag, boolean active, String note) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO " + tableName + " (uuid, department_id, flag, active, note, updated_at) VALUES (?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE active=VALUES(active), note=VALUES(note), updated_at=VALUES(updated_at)";
            try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
                st.setString(1, uuid.toString());
                st.setString(2, deptId);
                st.setString(3, flag);
                st.setInt(4, active ? 1 : 0);
                st.setString(5, note);
                st.setLong(6, System.currentTimeMillis());
                st.executeUpdate();
                return true;
            } catch (SQLException e) { return false; }
        });
    }

    public CompletableFuture<Map<String, Boolean>> flagsFor(UUID uuid, String deptId) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Boolean> out = new LinkedHashMap<>();
            String sql = "SELECT flag, active FROM " + tableName + " WHERE uuid=? AND department_id=?";
            try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
                st.setString(1, uuid.toString());
                st.setString(2, deptId);
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) out.put(rs.getString("flag"), rs.getInt("active") == 1);
                }
            } catch (SQLException ignored) {}
            return out;
        });
    }
}