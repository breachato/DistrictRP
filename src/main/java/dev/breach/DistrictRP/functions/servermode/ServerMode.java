package dev.breach.DistrictRP.functions.servermode;

public enum ServerMode {
    LOBBY,
    CREATIVE,
    ROLEPLAY,
    OFF;

    public static ServerMode fromString(String s) {
        if (s == null) return OFF;
        try {
            return ServerMode.valueOf(s.trim().toUpperCase());
        } catch (Exception e) {
            return OFF;
        }
    }
}