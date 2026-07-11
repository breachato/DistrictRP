package dev.breach.DistrictRP.database;

import dev.breach.DistrictRP.DistrictRP;

public class YamlDataStore implements DataStore {

    private final DistrictRP plugin;
    private boolean ready = false;

    public YamlDataStore(DistrictRP plugin) {
        this.plugin = plugin;
    }

    @Override
    public StorageType getType() {
        return StorageType.YAML;
    }

    @Override
    public void initialize() {
        this.ready = true;
        plugin.getLogger().info("[Database] YAML storage inizializzato (fallback).");
    }

    @Override
    public void shutdown() {
        this.ready = false;
    }

    @Override
    public boolean isReady() {
        return ready;
    }
}