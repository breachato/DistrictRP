package dev.breach.DistrictRP.database;

import dev.breach.DistrictRP.DistrictRP;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

public class DatabaseManager {

    private final DistrictRP plugin;
    private DataStore dataStore;
    private StorageType type;

    private final Map<String, DatabaseTable> tables = new LinkedHashMap<>();

    public DatabaseManager(DistrictRP plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("storage");
        String typeStr = sec != null ? sec.getString("type", "YAML") : "YAML";
        this.type = StorageType.fromString(typeStr);

        plugin.getLogger().info("[Database] Storage type richiesto: " + type.name());

        if (type == StorageType.MARIADB) {
            try {
                ConfigurationSection dbSec = plugin.getConfig().getConfigurationSection("storage.mariadb");
                DatabaseConfig cfg = new DatabaseConfig(dbSec);
                this.dataStore = new MariaDBDataStore(plugin, cfg);
                this.dataStore.initialize();
                if (!dataStore.isReady()) throw new IllegalStateException("MariaDB non pronto.");
                return true;
            } catch (Throwable t) {
                plugin.getLogger().severe("[Database] Fallback a YAML per errore MariaDB: " + t.getMessage());
                this.type = StorageType.YAML;
                this.dataStore = new YamlDataStore(plugin);
                try { this.dataStore.initialize(); } catch (Throwable ignored) {}
                return false;
            }
        } else {
            this.dataStore = new YamlDataStore(plugin);
            try { this.dataStore.initialize(); } catch (Throwable ignored) {}
            return true;
        }
    }

    public void registerTable(String key, DatabaseTable table) {
        tables.put(key, table);
        table.createIfNotExists();
    }

    public DatabaseTable getTable(String key) {
        return tables.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T extends DatabaseTable> T getTable(String key, Class<T> clazz) {
        DatabaseTable t = tables.get(key);
        if (t == null) return null;
        if (!clazz.isInstance(t)) return null;
        return (T) t;
    }

    public void shutdown() {
        if (dataStore != null) dataStore.shutdown();
    }

    public boolean isMariaDb() {
        return type == StorageType.MARIADB && dataStore != null && dataStore.isReady();
    }

    public boolean isYaml() {
        return type == StorageType.YAML;
    }

    public DataStore getDataStore() { return dataStore; }
    public StorageType getType() { return type; }
    public Map<String, DatabaseTable> getAllTables() { return new LinkedHashMap<>(tables); }
}