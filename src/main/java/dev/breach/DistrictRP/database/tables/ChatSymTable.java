package dev.breach.DistrictRP.database.tables;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.DataStore;
import dev.breach.DistrictRP.database.DatabaseTable;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ChatSymTable extends DatabaseTable {

    public ChatSymTable(DistrictRP plugin, DataStore store) { super(plugin, store, "chatsym"); }

    @Override
    protected String getCreateTableQuery() {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "symbol VARCHAR(8) PRIMARY KEY," +
                "command VARCHAR(64) NOT NULL," +
                "created_at BIGINT NOT NULL" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
    }

    public CompletableFuture<Boolean> upsert(String symbol, String command) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO " + tableName + " (symbol, command, created_at) VALUES (?,?,?) " +
                    "ON DUPLICATE KEY UPDATE command=VALUES(command)";
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, symbol);
                ps.setString(2, command);
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
                return true;
            } catch (SQLException e) { return false; }
        });
    }

    public CompletableFuture<Boolean> delete(String symbol) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = getConnection();
                 PreparedStatement ps = c.prepareStatement("DELETE FROM " + tableName + " WHERE symbol=?")) {
                ps.setString(1, symbol);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) { return false; }
        });
    }

    public CompletableFuture<Map<String, String>> all() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> out = new HashMap<>();
            try (Connection c = getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT symbol, command FROM " + tableName)) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.put(rs.getString(1), rs.getString(2));
                }
            } catch (SQLException ignored) {}
            return out;
        });
    }
}