package dev.breach.DistrictRP.commands.roleplay.ticket;

import java.util.UUID;

public class TicketComment {

    private final UUID commenter;
    private final String commenterName;
    private String text;
    private final long timestamp;
    private final boolean staff;
    private boolean cancelled;

    public TicketComment(UUID commenter, String commenterName, String text, long timestamp, boolean staff) {
        this.commenter = commenter;
        this.commenterName = commenterName;
        this.text = text;
        this.timestamp = timestamp;
        this.staff = staff;
        this.cancelled = false;
    }

    public UUID getCommenter() { return commenter; }
    public String getCommenterName() { return commenterName; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public long getTimestamp() { return timestamp; }
    public boolean isStaff() { return staff; }
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    public String serialize() {
        return commenter.toString() + ";" + commenterName + ";" + timestamp + ";"
                + staff + ";" + cancelled + ";" + text;
    }

    public static TicketComment deserialize(String s) {
        String[] parts = s.split(";", 6);
        if (parts.length < 6) {
            if (parts.length == 4) {
                TicketComment c = new TicketComment(UUID.fromString(parts[0]), parts[1], parts[3],
                        Long.parseLong(parts[2]), false);
                return c;
            }
            return null;
        }
        TicketComment c = new TicketComment(UUID.fromString(parts[0]), parts[1], parts[5],
                Long.parseLong(parts[2]), Boolean.parseBoolean(parts[3]));
        c.setCancelled(Boolean.parseBoolean(parts[4]));
        return c;
    }
}