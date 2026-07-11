package dev.breach.DistrictRP.database.tables;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.DataStore;
import dev.breach.DistrictRP.database.DatabaseTable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LogsTable extends DatabaseTable {

    public LogsTable(DistrictRP plugin, DataStore store) { super(plugin, store, "logs"); }

    @Override
    protected String getCreateTableQuery() {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "module VARCHAR(32) NOT NULL," +
                "player_uuid VARCHAR(36) DEFAULT NULL," +
                "player_name VARCHAR(32) DEFAULT NULL," +
                "action TEXT NOT NULL," +
                "server_id VARCHAR(64) DEFAULT NULL," +
                "created_at BIGINT NOT NULL," +
                "INDEX idx_module (module)," +
                "INDEX idx_player (player_uuid)," +
                "INDEX idx_time (created_at)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
    }

    public CompletableFuture<Long> add(String module, UUID uuid, String name, String action, String serverId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO " + tableName + " (module, player_uuid, player_name, action, server_id, created_at) VALUES (?,?,?,?,?,?)";
            try (Connection c = getConnection();
                 PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, module);
                ps.setString(2, uuid == null ? null : uuid.toString());
                ps.setString(3, name);
                ps.setString(4, action);
                ps.setString(5, serverId);
                ps.setLong(6, System.currentTimeMillis());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) return keys.getLong(1);
                }
            } catch (SQLException ignored) {}
            return -1L;
        });
    }

    public CompletableFuture<List<Row>> query(String module, UUID uuid, int limit, int offset) {
        return CompletableFuture.supplyAsync(() -> {
            List<Row> out = new ArrayList<>();
            StringBuilder sql = new StringBuilder("SELECT * FROM " + tableName + " WHERE 1=1");
            if (module != null) sql.append(" AND module=?");
            if (uuid != null) sql.append(" AND player_uuid=?");
            sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
                int i = 1;
                if (module != null) ps.setString(i++, module);
                if (uuid != null) ps.setString(i++, uuid.toString());
                ps.setInt(i++, limit);
                ps.setInt(i, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Row r = new Row();
                        r.id = rs.getLong("id");
                        r.module = rs.getString("module");
                        String pu = rs.getString("player_uuid");
                        r.playerUuid = pu != null ? UUID.fromString(pu) : null;
                        r.playerName = rs.getString("player_name");
                        r.action = rs.getString("action");
                        r.serverId = rs.getString("server_id");
                        r.createdAt = rs.getLong("created_at");
                        out.add(r);
                    }
                }
            } catch (SQLException ignored) {}
            return out;
        });
    }

    public static class Row {
        public long id;
        public String module, playerName, action, serverId;
        public UUID playerUuid;
        public long createdAt;
    }
}