package dev.breach.DistrictRP.commands.roleplay.plot;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.servermode.ServerModeManager;
import org.bukkit.Bukkit;

public class PlotAddon {

    private final DistrictRP plugin;
    private final ServerModeManager serverMode;

    private PlotSquaredHook hook;
    private PlotListener listener;

    public PlotAddon(DistrictRP plugin, ServerModeManager serverMode) {
        this.plugin = plugin;
        this.serverMode = serverMode;
    }

    public void enable() {
        hook = new PlotSquaredHook(plugin);
        if (!hook.isAvailable()) {
            plugin.getLogger().info("[PlotAddon] PlotSquared non presente, addon disabilitato.");
            return;
        }

        listener = new PlotListener(plugin, hook, serverMode);
        Bukkit.getPluginManager().registerEvents(listener, plugin);

        PlotCommand cmd = new PlotCommand(plugin, hook);
        if (plugin.getCommand("plot") != null) {
            plugin.getCommand("plot").setExecutor(cmd);
            plugin.getCommand("plot").setTabCompleter(cmd);
        }

        plugin.getLogger().info("[PlotAddon] Attivato correttamente.");
    }

    public void disable() {
    }

    public PlotSquaredHook getHook() { return hook; }
}