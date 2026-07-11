package dev.breach.DistrictRP.database.tables;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.DataStore;
import dev.breach.DistrictRP.database.DatabaseTable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TicketCommentsTable extends DatabaseTable {

    public TicketCommentsTable(DistrictRP plugin, DataStore store) {
        super(plugin, store, "ticket_comments");
    }

    @Override
    protected String getCreateTableQuery() {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "ticket_id INT NOT NULL," +
                "author_uuid VARCHAR(36) NOT NULL," +
                "author_name VARCHAR(32) NOT NULL," +
                "is_staff BOOLEAN NOT NULL DEFAULT FALSE," +
                "content TEXT NOT NULL," +
                "created_at BIGINT NOT NULL," +
                "INDEX idx_ticket (ticket_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
    }

    public CompletableFuture<Integer> add(int ticketId, UUID uuid, String name, boolean staff, String content) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO " + tableName +
                    " (ticket_id, author_uuid, author_name, is_staff, content, created_at) VALUES (?,?,?,?,?,?)";
            try (Connection c = getConnection();
                 PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, ticketId);
                ps.setString(2, uuid.toString());
                ps.setString(3, name);
                ps.setBoolean(4, staff);
                ps.setString(5, content);
                ps.setLong(6, System.currentTimeMillis());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) return keys.getInt(1);
                }
                return -1;
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB comments.add] " + e.getMessage());
                return -1;
            }
        });
    }

    public CompletableFuture<List<CommentRow>> listByTicket(int ticketId) {
        return CompletableFuture.supplyAsync(() -> {
            List<CommentRow> out = new ArrayList<>();
            String sql = "SELECT * FROM " + tableName + " WHERE ticket_id=? ORDER BY created_at ASC";
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, ticketId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        CommentRow r = new CommentRow();
                        r.id = rs.getInt("id");
                        r.ticketId = rs.getInt("ticket_id");
                        r.authorUuid = UUID.fromString(rs.getString("author_uuid"));
                        r.authorName = rs.getString("author_name");
                        r.staff = rs.getBoolean("is_staff");
                        r.content = rs.getString("content");
                        r.createdAt = rs.getLong("created_at");
                        out.add(r);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB comments.listByTicket] " + e.getMessage());
            }
            return out;
        });
    }

    public CompletableFuture<Boolean> delete(int commentId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = getConnection();
                 PreparedStatement ps = c.prepareStatement("DELETE FROM " + tableName + " WHERE id=?")) {
                ps.setInt(1, commentId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                return false;
            }
        });
    }

    public static class CommentRow {
        public int id;
        public int ticketId;
        public UUID authorUuid;
        public String authorName;
        public boolean staff;
        public String content;
        public long createdAt;
    }
}