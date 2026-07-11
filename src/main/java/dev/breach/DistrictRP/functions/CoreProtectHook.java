package dev.breach.DistrictRP.functions;

import dev.breach.DistrictRP.DistrictRP;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;

public class CoreProtectHook {

    private final DistrictRP plugin;
    private Object api;
    private boolean available = false;

    private Method mLogPlacement;
    private Method mLogRemoval;
    private Method mLogInteraction;
    private Method mLogContainerTransaction;
    private Method mLogChat;
    private Method mLogCommand;
    private Method mPerformLookup;
    private Method mPerformRestore;
    private Method mPerformRollback;

    public CoreProtectHook(DistrictRP plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        try {
            Plugin p = plugin.getServer().getPluginManager().getPlugin("CoreProtect");
            if (p == null || !p.isEnabled()) {
                plugin.getLogger().info("[CoreProtect] Plugin non trovato.");
                return;
            }

            Method getAPI = p.getClass().getMethod("getAPI");
            Object apiInstance = getAPI.invoke(p);
            if (apiInstance == null) {
                plugin.getLogger().warning("[CoreProtect] API null.");
                return;
            }

            try {
                Method isEnabledM = apiInstance.getClass().getMethod("isEnabled");
                if (!Boolean.TRUE.equals(isEnabledM.invoke(apiInstance))) {
                    plugin.getLogger().warning("[CoreProtect] API disabled.");
                    return;
                }
            } catch (Throwable ignored) {}

            try {
                Method apiVersion = apiInstance.getClass().getMethod("APIVersion");
                Object ver = apiVersion.invoke(apiInstance);
                if (ver instanceof Integer && ((Integer) ver) < 9) {
                    plugin.getLogger().warning("[CoreProtect] API v" + ver + " troppo vecchia.");
                    return;
                }
                plugin.getLogger().info("[CoreProtect] API v" + ver + " rilevata.");
            } catch (Throwable ignored) {}

            this.api = apiInstance;

            try { mLogPlacement = apiInstance.getClass().getMethod("logPlacement",
                    String.class, Location.class, Material.class, BlockData.class); } catch (Throwable ignored) {}
            try { mLogRemoval = apiInstance.getClass().getMethod("logRemoval",
                    String.class, Location.class, Material.class, BlockData.class); } catch (Throwable ignored) {}
            try { mLogInteraction = apiInstance.getClass().getMethod("logInteraction",
                    String.class, Location.class); } catch (Throwable ignored) {}
            try { mLogContainerTransaction = apiInstance.getClass().getMethod("logContainerTransaction",
                    String.class, Location.class); } catch (Throwable ignored) {}
            try { mLogChat = apiInstance.getClass().getMethod("logChat",
                    String.class, String.class); } catch (Throwable ignored) {}
            try { mLogCommand = apiInstance.getClass().getMethod("logCommand",
                    String.class, String.class); } catch (Throwable ignored) {}
            try { mPerformLookup = apiInstance.getClass().getMethod("performLookup",
                    int.class, List.class, List.class, List.class, List.class, List.class, int.class, Location.class); } catch (Throwable ignored) {}
            try { mPerformRestore = apiInstance.getClass().getMethod("performRestore",
                    int.class, List.class, List.class, List.class, List.class, List.class, int.class, Location.class); } catch (Throwable ignored) {}
            try { mPerformRollback = apiInstance.getClass().getMethod("performRollback",
                    int.class, List.class, List.class, List.class, List.class, List.class, int.class, Location.class); } catch (Throwable ignored) {}

            this.available = true;
            plugin.getLogger().info("[CoreProtect] Hook completato correttamente.");
        } catch (Throwable t) {
            plugin.getLogger().warning("[CoreProtect] Setup fallito: " + t.getMessage());
        }
    }

    public boolean isAvailable() { return available; }
    public Object getRawApi() { return api; }

    public void logPlacement(Player player, Location loc, Material material, BlockData data) {
        if (!available || mLogPlacement == null) return;
        try { mLogPlacement.invoke(api, player.getName(), loc, material, data); } catch (Throwable ignored) {}
    }

    public void logPlacement(String name, Location loc, Material material, BlockData data) {
        if (!available || mLogPlacement == null) return;
        try { mLogPlacement.invoke(api, name, loc, material, data); } catch (Throwable ignored) {}
    }

    public void logRemoval(Player player, Location loc, Material material, BlockData data) {
        if (!available || mLogRemoval == null) return;
        try { mLogRemoval.invoke(api, player.getName(), loc, material, data); } catch (Throwable ignored) {}
    }

    public void logRemoval(String name, Location loc, Material material, BlockData data) {
        if (!available || mLogRemoval == null) return;
        try { mLogRemoval.invoke(api, name, loc, material, data); } catch (Throwable ignored) {}
    }

    public void logInteraction(Player player, Location loc) {
        if (!available || mLogInteraction == null) return;
        try { mLogInteraction.invoke(api, player.getName(), loc); } catch (Throwable ignored) {}
    }

    public void logContainerTransaction(Player player, Location loc) {
        if (!available || mLogContainerTransaction == null) return;
        try { mLogContainerTransaction.invoke(api, player.getName(), loc); } catch (Throwable ignored) {}
    }

    public void logChat(Player player, String message) {
        if (!available || mLogChat == null) return;
        try { mLogChat.invoke(api, player.getName(), message); } catch (Throwable ignored) {}
    }

    public void logCommand(Player player, String command) {
        if (!available || mLogCommand == null) return;
        try { mLogCommand.invoke(api, player.getName(), command); } catch (Throwable ignored) {}
    }

    public void logCustomAction(Player player, String action) {
        logCommand(player, "[DistrictRP] " + action);
    }

    public void logCustomAction(String playerName, String action) {
        if (!available || mLogCommand == null) return;
        try { mLogCommand.invoke(api, playerName, "[DistrictRP] " + action); } catch (Throwable ignored) {}
    }
}