package dev.breach.DistrictRP.commands.roleplay.logs;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.repository.LogsRepository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LogsAPI {

    private final DistrictRP plugin;
    private final Map<String, LogModule> modules = new LinkedHashMap<>();

    private LogsRepository repo;
    private boolean useDb;

    public LogsAPI(DistrictRP plugin) {
        this.plugin = plugin;
        this.repo = new LogsRepository(plugin);
        this.useDb = repo.isAvailable();
        if (useDb) plugin.getLogger().info("[Logs] Storage: MariaDB");
        else plugin.getLogger().info("[Logs] Storage: YAML");
    }

    public void register(LogModule module) {
        modules.put(module.getId().toLowerCase(), module);
        plugin.getLogger().info("[Logs] Modulo registrato: " + module.getId());
    }

    public void unregister(String id) {
        modules.remove(id.toLowerCase());
    }

    public LogModule get(String id) {
        return modules.get(id.toLowerCase());
    }

    public boolean has(String id) {
        return modules.containsKey(id.toLowerCase());
    }

    public Collection<LogModule> getAll() {
        return modules.values();
    }

    public CompletableFuture<Long> log(String module, UUID uuid, String name, String action) {
        if (useDb) return repo.log(module, uuid, name, action);
        return CompletableFuture.completedFuture(-1L);
    }

    public CompletableFuture<List<dev.breach.DistrictRP.database.tables.LogsTable.Row>> query(
            String module, UUID uuid, int limit, int offset) {
        if (useDb) return repo.query(module, uuid, limit, offset);
        return CompletableFuture.completedFuture(new java.util.ArrayList<>());
    }

    public boolean isUsingDatabase() { return useDb; }
    public LogsRepository getRepository() { return repo; }
}