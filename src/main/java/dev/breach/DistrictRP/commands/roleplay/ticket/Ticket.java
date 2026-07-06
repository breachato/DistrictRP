package dev.breach.DistrictRP.commands.roleplay.ticket;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Ticket {

    private final int id;
    private final UUID author;
    private final String authorName;
    private String category;
    private final String reason;
    private final long openedAt;
    private boolean open;
    private UUID closedBy;
    private String closedByName;
    private String closeReason;
    private long closedAt;
    private UUID claimedBy;
    private String claimedByName;
    private final List<TicketComment> comments = new ArrayList<>();

    public Ticket(int id, UUID author, String authorName, String category, String reason, long openedAt) {
        this.id = id;
        this.author = author;
        this.authorName = authorName;
        this.category = category;
        this.reason = reason;
        this.openedAt = openedAt;
        this.open = true;
    }

    public int getId() { return id; }
    public UUID getAuthor() { return author; }
    public String getAuthorName() { return authorName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getReason() { return reason; }
    public long getOpenedAt() { return openedAt; }
    public boolean isOpen() { return open; }
    public void setOpen(boolean open) { this.open = open; }
    public UUID getClosedBy() { return closedBy; }
    public void setClosedBy(UUID closedBy) { this.closedBy = closedBy; }
    public String getClosedByName() { return closedByName; }
    public void setClosedByName(String closedByName) { this.closedByName = closedByName; }
    public String getCloseReason() { return closeReason; }
    public void setCloseReason(String closeReason) { this.closeReason = closeReason; }
    public long getClosedAt() { return closedAt; }
    public void setClosedAt(long closedAt) { this.closedAt = closedAt; }
    public UUID getClaimedBy() { return claimedBy; }
    public void setClaimedBy(UUID claimedBy) { this.claimedBy = claimedBy; }
    public String getClaimedByName() { return claimedByName; }
    public void setClaimedByName(String claimedByName) { this.claimedByName = claimedByName; }
    public boolean isClaimed() { return claimedBy != null; }
    public List<TicketComment> getComments() { return comments; }
    public void addComment(TicketComment comment) { comments.add(comment); }
    public int getCommentCount() { return comments.size(); }
}