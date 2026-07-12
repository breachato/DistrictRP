package dev.breach.DistrictRP.database.tables;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.DataStore;
import dev.breach.DistrictRP.database.DatabaseTable;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class StaffAccountsTable extends DatabaseTable {

    public StaffAccountsTable(DistrictRP plugin, DataStore store) {
        super(plugin, store, "staff_accounts");
    }

    @Override
    protected String getCreateTableQuery() {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "username VARCHAR(64) NOT NULL, " +
                "email VARCHAR(190) UNIQUE, " +
                "password_hash VARCHAR(255), " +
                "role VARCHAR(64) DEFAULT 'staff', " +
                "created_at BIGINT DEFAULT 0, " +
                "updated_at BIGINT DEFAULT 0, " +
                "last_login_at BIGINT DEFAULT 0" +
                ") DEFAULT CHARSET=utf8mb4";
    }

    public CompletableFuture<Boolean> upsert(UUID uuid, String username, String email, String hash) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO " + tableName + " (uuid, username, email, password_hash, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE username=VALUES(username), email=VALUES(email), password_hash=VALUES(password_hash), updated_at=VALUES(updated_at)";
            try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
                long now = System.currentTimeMillis();
                st.setString(1, uuid.toString());
                st.setString(2, username);
                st.setString(3, email);
                st.setString(4, hash);
                st.setLong(5, now);
                st.setLong(6, now);
                st.executeUpdate();
                return true;
            } catch (SQLException e) {
                plugin.getLogger().warning("[staff_accounts] upsert: " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> updatePasswordByEmail(String email, String hash) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE " + tableName + " SET password_hash=?, updated_at=? WHERE email=?";
            try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
                st.setString(1, hash);
                st.setLong(2, System.currentTimeMillis());
                st.setString(3, email);
                return st.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().warning("[staff_accounts] updatePassword: " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Map<String, Object>> findByEmail(String email) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT uuid, username, email, password_hash, role FROM " + tableName + " WHERE email=?";
            try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
                st.setString(1, email);
                try (ResultSet rs = st.executeQuery()) {
                    if (!rs.next()) return null;
                    Map<String, Object> out = new HashMap<>();
                    out.put("uuid", rs.getString("uuid"));
                    out.put("username", rs.getString("username"));
                    out.put("email", rs.getString("email"));
                    out.put("password_hash", rs.getString("password_hash"));
                    out.put("role", rs.getString("role"));
                    return out;
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("[staff_accounts] findByEmail: " + e.getMessage());
                return null;
            }
        });
    }

    public CompletableFuture<Void> touchLastLogin(String uuid) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE " + tableName + " SET last_login_at=? WHERE uuid=?";
            try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
                st.setLong(1, System.currentTimeMillis());
                st.setString(2, uuid);
                st.executeUpdate();
            } catch (SQLException ignored) {}
        });
    }
}