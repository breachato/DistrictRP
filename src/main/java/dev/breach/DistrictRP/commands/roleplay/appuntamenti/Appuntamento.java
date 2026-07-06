package dev.breach.DistrictRP.commands.roleplay.appuntamenti;

import java.util.UUID;

public class Appuntamento {

    private final int id;
    private final UUID player;
    private final String playerName;
    private final String reparto;
    private final String giorno;
    private final String orario;
    private final long createdAt;

    public Appuntamento(int id, UUID player, String playerName, String reparto, String giorno, String orario, long createdAt) {
        this.id = id;
        this.player = player;
        this.playerName = playerName;
        this.reparto = reparto;
        this.giorno = giorno;
        this.orario = orario;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public UUID getPlayer() { return player; }
    public String getPlayerName() { return playerName; }
    public String getReparto() { return reparto; }
    public String getGiorno() { return giorno; }
    public String getOrario() { return orario; }
    public long getCreatedAt() { return createdAt; }
}