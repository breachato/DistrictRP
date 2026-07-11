package dev.breach.DistrictRP.functions;

import dev.breach.DistrictRP.DistrictRP;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

public class CoreProtectAutoLogger implements Listener {

    private final DistrictRP plugin;
    private final CoreProtectHook hook;

    public CoreProtectAutoLogger(DistrictRP plugin, CoreProtectHook hook) {
        this.plugin = plugin;
        this.hook = hook;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!plugin.getConfig().getBoolean("coreprotect.log-chat", true)) return;
        if (!hook.isAvailable()) return;
        hook.logChat(event.getPlayer(), event.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfig().getBoolean("coreprotect.log-commands", true)) return;
        if (!hook.isAvailable()) return;
        hook.logCommand(event.getPlayer(), event.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTp(PlayerTeleportEvent event) {
        if (!plugin.getConfig().getBoolean("coreprotect.log-teleports", true)) return;
        if (!hook.isAvailable()) return;
        Player p = event.getPlayer();
        String reason = event.getCause() != null ? event.getCause().name() : "?";
        hook.logCustomAction(p, "tp [" + reason + "] " +
                (event.getTo() != null ? event.getTo().getBlockX() + "," +
                        event.getTo().getBlockY() + "," + event.getTo().getBlockZ() : "?"));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!plugin.getConfig().getBoolean("coreprotect.log-containers", true)) return;
        if (!hook.isAvailable()) return;
        if (!(event.getPlayer() instanceof Player p)) return;
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder == null) return;
        try {
            org.bukkit.Location loc = event.getInventory().getLocation();
            if (loc != null) hook.logContainerTransaction(p, loc);
        } catch (Throwable ignored) {}
    }
}