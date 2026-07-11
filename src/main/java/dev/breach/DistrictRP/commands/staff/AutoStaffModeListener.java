package dev.breach.DistrictRP.commands.staff;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.servermode.ServerMode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class AutoStaffModeListener implements Listener {

    private final DistrictRP plugin;

    public AutoStaffModeListener(DistrictRP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("staffmode.auto-staffmode-enabled", true)) return;
        Player p = event.getPlayer();

        String perm = plugin.getConfig().getString(
                "staffmode.auto-staffmode-permission", "DistrictRP.autostaffmode");
        if (!p.hasPermission(perm)) return;

        if (plugin.getServerModeManager() == null) return;
        if (plugin.getServerModeManager().getCurrent() != ServerMode.ROLEPLAY) return;

        long delay = plugin.getConfig().getLong("staffmode.auto-staffmode-delay-ticks", 20L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            if (plugin.getStaffModeManager() == null) return;
            if (plugin.getStaffModeManager().isInStaffMode(p)) return;
            plugin.getStaffModeManager().enter(p);
            dev.breach.DistrictRP.functions.MessageUtils.sendMsg(p, "staffmode.auto-entered");
        }, delay);
    }
}