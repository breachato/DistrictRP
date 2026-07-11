package dev.breach.DistrictRP.database.tables;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.DataStore;
import dev.breach.DistrictRP.database.DatabaseTable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TicketsTable extends DatabaseTable {

    public TicketsTable(DistrictRP plugin, DataStore store) {
        super(plugin, store, "tickets");
    }

    @Override
    protected String getCreateTableQuery() {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "author_uuid VARCHAR(36) NOT NULL," +
                "author_name VARCHAR(32) NOT NULL," +
                "category VARCHAR(64) NOT NULL," +
                "reason TEXT NOT NULL," +
                "state VARCHAR(16) NOT NULL DEFAULT 'OPEN'," +
                "claimed_by_uuid VARCHAR(36) DEFAULT NULL," +
                "claimed_by_name VARCHAR(32) DEFAULT NULL," +
                "closed_by_uuid VARCHAR(36) DEFAULT NULL," +
                "closed_by_name VARCHAR(32) DEFAULT NULL," +
                "close_reason TEXT DEFAULT NULL," +
                "created_at BIGINT NOT NULL," +
                "updated_at BIGINT NOT NULL," +
                "closed_at BIGINT DEFAULT NULL," +
                "server_origin VARCHAR(32) DEFAULT NULL," +
                "INDEX idx_author (author_uuid)," +
                "INDEX idx_state (state)," +
                "INDEX idx_category (category)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
    }

    public CompletableFuture<Integer> insert(UUID authorUuid, String authorName, String category,
                                             String reason, String serverOrigin) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO " + tableName +
                    " (author_uuid, author_name, category, reason, state, created_at, updated_at, server_origin)" +
                    " VALUES (?, ?, ?, ?, 'OPEN', ?, ?, ?)";
            try (Connection c = getConnection();
                 PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                long now = System.currentTimeMillis();
                ps.setString(1, authorUuid.toString());
                ps.setString(2, authorName);
                ps.setString(3, category);
                ps.setString(4, reason);
                ps.setLong(5, now);
                ps.setLong(6, now);
                ps.setString(7, serverOrigin);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) return keys.getInt(1);
                }
                return -1;
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB tickets.insert] " + e.getMessage());
                return -1;
            }
        });
    }

    public CompletableFuture<Boolean> updateState(int id, String state) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE " + tableName + " SET state=?, updated_at=? WHERE id=?";
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, state);
                ps.setLong(2, System.currentTimeMillis());
                ps.setInt(3, id);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB tickets.updateState] " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> claim(int id, UUID staffUuid, String staffName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE " + tableName + " SET claimed_by_uuid=?, claimed_by_name=?, updated_at=? WHERE id=?";
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, staffUuid.toString());
                ps.setString(2, staffName);
                ps.setLong(3, System.currentTimeMillis());
                ps.setInt(4, id);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB tickets.claim] " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> close(int id, UUID staffUuid, String staffName, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE " + tableName +
                    " SET state='CLOSED', closed_by_uuid=?, closed_by_name=?, close_reason=?, closed_at=?, updated_at=? WHERE id=?";
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                long now = System.currentTimeMillis();
                ps.setString(1, staffUuid.toString());
                ps.setString(2, staffName);
                ps.setString(3, reason);
                ps.setLong(4, now);
                ps.setLong(5, now);
                ps.setInt(6, id);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB tickets.close] " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<List<TicketRow>> listByAuthor(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> query(
                "SELECT * FROM " + tableName + " WHERE author_uuid=? ORDER BY created_at DESC",
                ps -> ps.setString(1, uuid.toString())
        ));
    }

    public CompletableFuture<List<TicketRow>> listByState(String state) {
        return CompletableFuture.supplyAsync(() -> query(
                "SELECT * FROM " + tableName + " WHERE state=? ORDER BY created_at DESC",
                ps -> ps.setString(1, state)
        ));
    }

    public CompletableFuture<TicketRow> get(int id) {
        return CompletableFuture.supplyAsync(() -> {
            List<TicketRow> list = query(
                    "SELECT * FROM " + tableName + " WHERE id=? LIMIT 1",
                    ps -> ps.setInt(1, id)
            );
            return list.isEmpty() ? null : list.get(0);
        });
    }

    public CompletableFuture<Integer> countOpenByAuthor(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE author_uuid=? AND state='OPEN'";
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
                return 0;
            } catch (SQLException e) {
                return 0;
            }
        });
    }

    private List<TicketRow> query(String sql, SqlSetter setter) {
        List<TicketRow> out = new ArrayList<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(fromRs(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[DB tickets.query] " + e.getMessage());
        }
        return out;
    }

    private TicketRow fromRs(ResultSet rs) throws SQLException {
        TicketRow r = new TicketRow();
        r.id = rs.getInt("id");
        r.authorUuid = UUID.fromString(rs.getString("author_uuid"));
        r.authorName = rs.getString("author_name");
        r.category = rs.getString("category");
        r.reason = rs.getString("reason");
        r.state = rs.getString("state");
        String cu = rs.getString("claimed_by_uuid");
        r.claimedByUuid = cu != null ? UUID.fromString(cu) : null;
        r.claimedByName = rs.getString("claimed_by_name");
        String cbu = rs.getString("closed_by_uuid");
        r.closedByUuid = cbu != null ? UUID.fromString(cbu) : null;
        r.closedByName = rs.getString("closed_by_name");
        r.closeReason = rs.getString("close_reason");
        r.createdAt = rs.getLong("created_at");
        r.updatedAt = rs.getLong("updated_at");
        long ca = rs.getLong("closed_at");
        r.closedAt = rs.wasNull() ? null : ca;
        r.serverOrigin = rs.getString("server_origin");
        return r;
    }

    @FunctionalInterface interface SqlSetter { void set(PreparedStatement ps) throws SQLException; }

    public static class TicketRow {
        public int id;
        public UUID authorUuid;
        public String authorName;
        public String category;
        public String reason;
        public String state;
        public UUID claimedByUuid;
        public String claimedByName;
        public UUID closedByUuid;
        public String closedByName;
        public String closeReason;
        public long createdAt;
        public long updatedAt;
        public Long closedAt;
        public String serverOrigin;
    }
}