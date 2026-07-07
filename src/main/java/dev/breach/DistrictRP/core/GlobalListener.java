package dev.breach.DistrictRP.core;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import dev.breach.DistrictRP.functions.servermode.ServerMode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class GlobalListener implements Listener {

    private final DistrictRP plugin;

    public GlobalListener(DistrictRP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        plugin.back.put(p.getUniqueId(), p.getLocation());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        Player p = e.getPlayer();
        plugin.back.put(p.getUniqueId(), e.getFrom());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (Boolean.TRUE.equals(plugin.godMode.get(p.getUniqueId()))) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.setJoinMessage(null);

        String joinMsg = MessageUtils.color(plugin.getConfig().getString("join.message", ""));
        if (joinMsg.isEmpty()) return;
        String finalMsg = joinMsg.replace("%player%", player.getName());

        ServerMode current = plugin.getServerModeManager() != null
                ? plugin.getServerModeManager().getCurrent()
                : ServerMode.OFF;

        if (current == ServerMode.LOBBY || current == ServerMode.CREATIVE) {
            Bukkit.broadcastMessage(finalMsg);
            return;
        }

        String staffPerm = plugin.getConfig().getString("staff-notify.permission", "DistrictRP.staff.notify");
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(staffPerm)) p.sendMessage(finalMsg);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        event.setQuitMessage(null);

        String msg = MessageUtils.color(plugin.getConfig().getString("quit.message", ""));
        if (msg.isEmpty()) return;
        String finalMsg = msg.replace("%player%", player.getName());

        ServerMode current = plugin.getServerModeManager() != null
                ? plugin.getServerModeManager().getCurrent()
                : ServerMode.OFF;

        if (current == ServerMode.LOBBY || current == ServerMode.CREATIVE) {
            Bukkit.broadcastMessage(finalMsg);
            return;
        }

        String staffPerm = plugin.getConfig().getString("staff-notify.permission", "DistrictRP.staff.notify");
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(staffPerm)) p.sendMessage(finalMsg);
        }
    }
}