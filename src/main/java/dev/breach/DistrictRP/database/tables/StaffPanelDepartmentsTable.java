package dev.breach.DistrictRP.database.tables;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.DataStore;
import dev.breach.DistrictRP.database.DatabaseTable;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class StaffPanelDepartmentsTable extends DatabaseTable {

    public StaffPanelDepartmentsTable(DistrictRP plugin, DataStore store) {
        super(plugin, store, "sp_departments");
    }

    @Override
    protected String getCreateTableQuery() {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "id VARCHAR(64) PRIMARY KEY, " +
                "name VARCHAR(128) NOT NULL, " +
                "color VARCHAR(16) DEFAULT '#c9a84c', " +
                "position INT DEFAULT 0" +
                ") DEFAULT CHARSET=utf8mb4";
    }

    public CompletableFuture<Boolean> createIfMissing(String id, String name, String color, int position) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT IGNORE INTO " + tableName + " (id, name, color, position) VALUES (?, ?, ?, ?)";
            try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
                st.setString(1, id);
                st.setString(2, name);
                st.setString(3, color);
                st.setInt(4, position);
                return st.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().warning("[sp_departments] createIfMissing: " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> upsert(String id, String name, String color, int position) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO " + tableName + " (id, name, color, position) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE name=VALUES(name), color=VALUES(color), position=VALUES(position)";
            try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
                st.setString(1, id);
                st.setString(2, name);
                st.setString(3, color);
                st.setInt(4, position);
                st.executeUpdate();
                return true;
            } catch (SQLException e) { return false; }
        });
    }

    public CompletableFuture<Boolean> delete(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = getConnection();
                 PreparedStatement st = c.prepareStatement("DELETE FROM " + tableName + " WHERE id=?")) {
                st.setString(1, id);
                return st.executeUpdate() > 0;
            } catch (SQLException e) { return false; }
        });
    }

    public CompletableFuture<List<Map<String, Object>>> listAll() {
        return CompletableFuture.supplyAsync(() -> {
            List<Map<String, Object>> out = new ArrayList<>();
            String sql = "SELECT id, name, color, position FROM " + tableName + " ORDER BY position ASC, name ASC";
            try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(sql);
                 ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getString("id"));
                    m.put("name", rs.getString("name"));
                    m.put("color", rs.getString("color"));
                    m.put("position", rs.getInt("position"));
                    out.add(m);
                }
            } catch (SQLException ignored) {}
            return out;
        });
    }
}