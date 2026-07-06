package dev.breach.DistrictRP.commands.roleplay.logs;

import dev.breach.DistrictRP.DistrictRP;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class LogsAPI {

    private final DistrictRP plugin;
    private final Map<String, LogModule> modules = new LinkedHashMap<>();

    public LogsAPI(DistrictRP plugin) {
        this.plugin = plugin;
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
}