package dev.breach.DistrictRP.framework;

import dev.breach.DistrictRP.DistrictRP;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModuleManager {

    private final DistrictRP core;
    private final Map<String, DistrictModule> modules = new LinkedHashMap<>();
    private final Map<String, List<Listener>> registeredListeners = new LinkedHashMap<>();

    public ModuleManager(DistrictRP core) {
        this.core = core;
    }

    public boolean registerModule(Plugin owningPlugin, DistrictModule module) {
        String id = module.id();
        if (id == null || id.isEmpty()) {
            core.getLogger().warning("[ModuleManager] Modulo con id null/vuoto rifiutato.");
            return false;
        }
        if (modules.containsKey(id)) {
            core.getLogger().warning("[ModuleManager] Modulo '" + id + "' già registrato.");
            return false;
        }

        try {
            ModuleContext ctx = new ModuleContext(core, owningPlugin, module);
            module.bindContext(ctx);
            module.setState(ModuleState.LOADED);

            try {
                module.onLoad();
            } catch (Throwable t) {
                core.getLogger().warning("[ModuleManager] onLoad '" + id + "' errore: " + t.getMessage());
            }

            module.onEnable();
            registerListeners(owningPlugin, module);
            ctx.setEnabled(true);
            module.setState(ModuleState.ENABLED);

            modules.put(id, module);
            core.getLogger().info("[ModuleManager] Modulo '" + id + "' abilitato.");
            return true;
        } catch (Throwable t) {
            core.getLogger().severe("[ModuleManager] Errore registrando '" + id + "': " + t.getMessage());
            module.setState(ModuleState.ERROR);
            return false;
        }
    }

    private void registerListeners(Plugin owningPlugin, DistrictModule module) {
        List<Listener> listeners = module.buildListeners();
        if (listeners == null || listeners.isEmpty()) return;
        for (Listener l : listeners) {
            if (l == null) continue;
            Bukkit.getPluginManager().registerEvents(l, owningPlugin);
        }
        registeredListeners.put(module.id(), listeners);
    }

    public boolean unregisterModule(String id) {
        DistrictModule module = modules.remove(id);
        if (module == null) return false;

        List<Listener> listeners = registeredListeners.remove(id);
        if (listeners != null) {
            for (Listener l : listeners) HandlerList.unregisterAll(l);
        }

        try {
            module.onDisable();
        } catch (Throwable t) {
            core.getLogger().warning("[ModuleManager] onDisable '" + id + "' errore: " + t.getMessage());
        }
        module.setState(ModuleState.DISABLED);
        if (module.ctx() != null) module.ctx().setEnabled(false);
        core.getLogger().info("[ModuleManager] Modulo '" + id + "' disabilitato.");
        return true;
    }

    public void reloadModule(String id) {
        DistrictModule module = modules.get(id);
        if (module == null) return;
        if (module.ctx() != null) module.ctx().reload();
        try {
            module.onReload();
        } catch (Throwable t) {
            core.getLogger().warning("[ModuleManager] onReload '" + id + "' errore: " + t.getMessage());
        }
    }

    public void reloadAll() {
        for (String id : modules.keySet()) reloadModule(id);
    }

    public void disableAll() {
        for (String id : new java.util.ArrayList<>(modules.keySet())) {
            unregisterModule(id);
        }
    }

    public DistrictModule getModule(String id) {
        return modules.get(id);
    }

    public boolean isEnabled(String id) {
        DistrictModule m = modules.get(id);
        return m != null && m.state() == ModuleState.ENABLED;
    }

    public Map<String, DistrictModule> getAllModules() {
        return new LinkedHashMap<>(modules);
    }
}