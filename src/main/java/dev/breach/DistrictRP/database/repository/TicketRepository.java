package dev.breach.DistrictRP.database.repository;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.roleplay.ticket.Ticket;
import dev.breach.DistrictRP.commands.roleplay.ticket.TicketComment;
import dev.breach.DistrictRP.database.Repository;
import dev.breach.DistrictRP.database.tables.TicketCommentsTable;
import dev.breach.DistrictRP.database.tables.TicketCommentsTable.CommentRow;
import dev.breach.DistrictRP.database.tables.TicketsTable;
import dev.breach.DistrictRP.database.tables.TicketsTable.TicketRow;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TicketRepository implements Repository {

    private final DistrictRP plugin;
    private TicketsTable table;
    private TicketCommentsTable commentsTable;

    public TicketRepository(DistrictRP plugin) {
        this.plugin = plugin;
        if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isMariaDb()) {
            this.table = plugin.getDatabaseManager().getTable("tickets", TicketsTable.class);
            this.commentsTable = plugin.getDatabaseManager().getTable("ticket_comments", TicketCommentsTable.class);
        }
    }

    @Override
    public boolean isAvailable() {
        return table != null && commentsTable != null;
    }

    public CompletableFuture<Integer> createTicket(Ticket t, String serverOrigin) {
        if (!isAvailable()) return CompletableFuture.completedFuture(-1);
        return table.insert(t.getAuthor(), t.getAuthorName(), t.getCategory(), t.getReason(), serverOrigin);
    }

    public CompletableFuture<Boolean> closeTicket(int id, UUID staff, String staffName, String reason) {
        if (!isAvailable()) return CompletableFuture.completedFuture(false);
        return table.close(id, staff, staffName, reason);
    }

    public CompletableFuture<Boolean> reopenTicket(int id) {
        if (!isAvailable()) return CompletableFuture.completedFuture(false);
        return table.updateState(id, "OPEN");
    }

    public CompletableFuture<Boolean> claimTicket(int id, UUID staff, String staffName) {
        if (!isAvailable()) return CompletableFuture.completedFuture(false);
        return table.claim(id, staff, staffName);
    }

    public CompletableFuture<Boolean> unclaimTicket(int id) {
        if (!isAvailable()) return CompletableFuture.completedFuture(false);
        return table.claim(id, new UUID(0L, 0L), null);
    }

    public CompletableFuture<Boolean> updateCategory(int id, String category) {
        if (!isAvailable()) return CompletableFuture.completedFuture(false);
        return CompletableFuture.supplyAsync(() -> {
            try (var c = plugin.getDatabaseManager().getDataStore().getConnection();
                 var ps = c.prepareStatement("UPDATE " + table.getTableName() +
                         " SET category=?, updated_at=? WHERE id=?")) {
                ps.setString(1, category);
                ps.setLong(2, System.currentTimeMillis());
                ps.setInt(3, id);
                return ps.executeUpdate() > 0;
            } catch (Exception e) {
                plugin.getLogger().warning("[TicketRepo] updateCategory: " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Integer> addComment(int ticketId, TicketComment c) {
        if (!isAvailable()) return CompletableFuture.completedFuture(-1);
        return commentsTable.add(ticketId, c.getCommenter(), c.getCommenterName(), c.isStaff(), c.getText());
    }

    public CompletableFuture<Boolean> cancelComment(int commentDbId, String replacementText) {
        if (!isAvailable()) return CompletableFuture.completedFuture(false);
        return CompletableFuture.supplyAsync(() -> {
            try (var c = plugin.getDatabaseManager().getDataStore().getConnection();
                 var ps = c.prepareStatement("UPDATE " + commentsTable.getTableName() +
                         " SET content=? WHERE id=?")) {
                ps.setString(1, replacementText);
                ps.setInt(2, commentDbId);
                return ps.executeUpdate() > 0;
            } catch (Exception e) {
                return false;
            }
        });
    }

    public CompletableFuture<Ticket> loadTicket(int id) {
        if (!isAvailable()) return CompletableFuture.completedFuture(null);
        return table.get(id).thenCompose(row -> {
            if (row == null) return CompletableFuture.completedFuture(null);
            return commentsTable.listByTicket(id).thenApply(commentRows -> buildTicket(row, commentRows));
        });
    }

    public CompletableFuture<List<Ticket>> fetchAll() {
        if (!isAvailable()) return CompletableFuture.completedFuture(new ArrayList<>());
        return CompletableFuture.supplyAsync(() -> {
            List<Ticket> out = new ArrayList<>();
            try (var c = plugin.getDatabaseManager().getDataStore().getConnection();
                 var ps = c.prepareStatement("SELECT * FROM " + table.getTableName() + " ORDER BY id ASC")) {
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) {
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

                        List<CommentRow> commentsList = commentsTable.listByTicket(r.id).join();
                        out.add(buildTicket(r, commentsList));
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[TicketRepo] loadAll: " + e.getMessage());
            }
            return out;
        });
    }

    private Ticket buildTicket(TicketRow r, List<CommentRow> commentRows) {
        Ticket t = new Ticket(r.id, r.authorUuid, r.authorName, r.category, r.reason, r.createdAt);
        t.setOpen("OPEN".equalsIgnoreCase(r.state));
        t.setClosedBy(r.closedByUuid);
        t.setClosedByName(r.closedByName);
        t.setCloseReason(r.closeReason);
        if (r.closedAt != null) t.setClosedAt(r.closedAt);
        if (r.claimedByUuid != null && !r.claimedByUuid.equals(new UUID(0L, 0L))) {
            t.setClaimedBy(r.claimedByUuid);
            t.setClaimedByName(r.claimedByName);
        }
        if (commentRows != null) {
            for (CommentRow cr : commentRows) {
                TicketComment tc = new TicketComment(
                        cr.authorUuid, cr.authorName, cr.content, cr.createdAt, cr.staff);
                t.addComment(tc);
            }
        }
        return t;
    }
}