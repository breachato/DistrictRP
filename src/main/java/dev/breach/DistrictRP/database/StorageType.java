package dev.breach.DistrictRP.database;

public enum StorageType {
    YAML,
    MARIADB;

    public static StorageType fromString(String s) {
        if (s == null) return YAML;
        try { return StorageType.valueOf(s.trim().toUpperCase()); }
        catch (Exception e) { return YAML; }
    }
}