package dev.breach.DistrictRP.functions.servermode;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class ServerModeListener implements Listener {

    private final DistrictRP plugin;
    private final ServerModeManager manager;

    public ServerModeListener(DistrictRP plugin, ServerModeManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) manager.handleJoin(event.getPlayer());
        }, 10L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCmd(PlayerCommandPreprocessEvent event) {
        if (manager.canBypass(event.getPlayer())) return;
        String msg = event.getMessage();
        if (manager.isCommandDisabled(msg)) {
            event.setCancelled(true);
            MessageUtils.sendMsg(event.getPlayer(), "servermode.command-disabled",
                    "mode", manager.getCurrentDisplay());
        }
    }
}