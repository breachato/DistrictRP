package dev.breach.DistrictRP.database.tables;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.DataStore;
import dev.breach.DistrictRP.database.DatabaseTable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AppuntamentiTable extends DatabaseTable {

    public AppuntamentiTable(DistrictRP plugin, DataStore store) { super(plugin, store, "appuntamenti"); }

    @Override
    protected String getCreateTableQuery() {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "player_uuid VARCHAR(36) NOT NULL," +
                "player_name VARCHAR(32) NOT NULL," +
                "reparto VARCHAR(64) NOT NULL," +
                "giorno VARCHAR(32) NOT NULL," +
                "orario VARCHAR(16) NOT NULL," +
                "created_at BIGINT NOT NULL," +
                "UNIQUE KEY uniq_slot (reparto, giorno, orario)," +
                "INDEX idx_player (player_uuid)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
    }

    public CompletableFuture<Integer> book(UUID uuid, String name, String reparto, String giorno, String orario) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO " + tableName +
                    " (player_uuid, player_name, reparto, giorno, orario, created_at) VALUES (?,?,?,?,?,?)";
            try (Connection c = getConnection();
                 PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setString(3, reparto);
                ps.setString(4, giorno);
                ps.setString(5, orario);
                ps.setLong(6, System.currentTimeMillis());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) return keys.getInt(1);
                }
                return -1;
            } catch (SQLIntegrityConstraintViolationException dup) {
                return -2;
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB appuntamenti.book] " + e.getMessage());
                return -1;
            }
        });
    }

    public CompletableFuture<Boolean> cancel(int id) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = getConnection();
                 PreparedStatement ps = c.prepareStatement("DELETE FROM " + tableName + " WHERE id=?")) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) { return false; }
        });
    }

    public CompletableFuture<List<Row>> byPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Row> out = new ArrayList<>();
            try (Connection c = getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM " + tableName + " WHERE player_uuid=? ORDER BY giorno, orario")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(fromRs(rs));
                }
            } catch (SQLException ignored) {}
            return out;
        });
    }

    public CompletableFuture<List<Row>> byReparto(String reparto) {
        return CompletableFuture.supplyAsync(() -> {
            List<Row> out = new ArrayList<>();
            try (Connection c = getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM " + tableName + " WHERE reparto=? ORDER BY giorno, orario")) {
                ps.setString(1, reparto);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(fromRs(rs));
                }
            } catch (SQLException ignored) {}
            return out;
        });
    }

    private Row fromRs(ResultSet rs) throws SQLException {
        Row r = new Row();
        r.id = rs.getInt("id");
        r.playerUuid = UUID.fromString(rs.getString("player_uuid"));
        r.playerName = rs.getString("player_name");
        r.reparto = rs.getString("reparto");
        r.giorno = rs.getString("giorno");
        r.orario = rs.getString("orario");
        r.createdAt = rs.getLong("created_at");
        return r;
    }

    public static class Row {
        public int id;
        public UUID playerUuid;
        public String playerName, reparto, giorno, orario;
        public long createdAt;
    }
}