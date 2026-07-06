package dev.breach.DistrictRP.commands.roleplay.logs;

import java.util.UUID;

public class LogEntry {

    private final int id;
    private final UUID player;
    private final String playerName;
    private final String action;
    private final long timestamp;

    public LogEntry(int id, UUID player, String playerName, String action, long timestamp) {
        this.id = id;
        this.player = player;
        this.playerName = playerName;
        this.action = action;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public UUID getPlayer() { return player; }
    public String getPlayerName() { return playerName; }
    public String getAction() { return action; }
    public long getTimestamp() { return timestamp; }
}