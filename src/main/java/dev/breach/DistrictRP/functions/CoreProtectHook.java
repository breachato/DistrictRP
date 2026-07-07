package dev.breach.DistrictRP.functions;

import dev.breach.DistrictRP.DistrictRP;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class CoreProtectHook {

    private final DistrictRP plugin;
    private CoreProtectAPI api;
    private boolean available = false;

    public CoreProtectHook(DistrictRP plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        try {
            Plugin p = plugin.getServer().getPluginManager().getPlugin("CoreProtect");
            if (!(p instanceof CoreProtect)) {
                plugin.getLogger().info("[CoreProtect] Plugin non trovato. Logging disabilitato.");
                return;
            }
            CoreProtectAPI a = ((CoreProtect) p).getAPI();
            if (!a.isEnabled()) {
                plugin.getLogger().warning("[CoreProtect] API non abilitata.");
                return;
            }
            if (a.APIVersion() < 9) {
                plugin.getLogger().warning("[CoreProtect] API troppo vecchia (< v9). Logging disabilitato.");
                return;
            }
            this.api = a;
            this.available = true;
            plugin.getLogger().info("[CoreProtect] Hook completato (API v" + a.APIVersion() + ").");
        } catch (Throwable t) {
            plugin.getLogger().warning("[CoreProtect] Setup fallito: " + t.getMessage());
        }
    }

    public boolean isAvailable() { return available; }
    public CoreProtectAPI getApi() { return api; }

    public void logPlacement(Player player, Location loc, Material material, BlockData data) {
        if (!available) return;
        try { api.logPlacement(player.getName(), loc, material, data); } catch (Throwable ignored) {}
    }

    public void logRemoval(Player player, Location loc, Material material, BlockData data) {
        if (!available) return;
        try { api.logRemoval(player.getName(), loc, material, data); } catch (Throwable ignored) {}
    }

    public void logInteraction(Player player, Location loc) {
        if (!available) return;
        try { api.logInteraction(player.getName(), loc); } catch (Throwable ignored) {}
    }

    public void logContainerTransaction(Player player, Location loc) {
        if (!available) return;
        try { api.logContainerTransaction(player.getName(), loc); } catch (Throwable ignored) {}
    }

    public void logChat(Player player, String message) {
        if (!available) return;
        try { api.logChat(player.getName(), message); } catch (Throwable ignored) {}
    }

    public void logCommand(Player player, String command) {
        if (!available) return;
        try { api.logCommand(player.getName(), command); } catch (Throwable ignored) {}
    }

    public void logCustomAction(Player player, String action) {
        if (!available) return;
        try { api.logCommand(player.getName(), "[DistrictRP] " + action); } catch (Throwable ignored) {}
    }
}