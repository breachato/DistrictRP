package dev.breach.DistrictRP.commands.roleplay.plot;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.servermode.ServerMode;
import dev.breach.DistrictRP.functions.servermode.ServerModeManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class PlotListener implements Listener {

    private final DistrictRP plugin;
    private final PlotSquaredHook hook;
    private final ServerModeManager serverMode;

    private final Map<UUID, Boolean> lastInPlot = new HashMap<>();
    private final Map<UUID, Boolean> lastRoleOwner = new HashMap<>();

    public PlotListener(DistrictRP plugin, PlotSquaredHook hook, ServerModeManager serverMode) {
        this.plugin = plugin;
        this.hook = hook;
        this.serverMode = serverMode;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;
        evaluate(event.getPlayer());
    }

    @EventHandler
    public void onTp(PlayerTeleportEvent event) {
        new BukkitRunnable() {
            @Override public void run() {
                if (event.getPlayer().isOnline()) evaluate(event.getPlayer());
            }
        }.runTaskLater(plugin, 5L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        new BukkitRunnable() {
            @Override public void run() {
                if (event.getPlayer().isOnline()) evaluate(event.getPlayer());
            }
        }.runTaskLater(plugin, 20L);
    }

    private void evaluate(Player p) {
        if (!hook.isAvailable()) return;
        if (!plugin.getConfig().getBoolean("plot.enabled", true)) return;

        String bypassPerm = plugin.getConfig().getString(
                "server-mode.bypass-permission", "DistrictRP.servermode.bypass");
        if (p.hasPermission(bypassPerm)) return;

        String onlyInModeStr = plugin.getConfig().getString(
                "plot.auto-gamemode.only-in-mode", "CREATIVE");
        ServerMode only = ServerMode.fromString(onlyInModeStr);
        if (serverMode.getCurrent() != only) return;

        String plotWorld = plugin.getConfig().getString("plot.plot-world-name", "plots");
        if (!p.getWorld().getName().equalsIgnoreCase(plotWorld)) return;

        boolean inPlot = hook.isInPlot(p);
        boolean owner = inPlot && hook.isPlotOwner(p);
        boolean trusted = inPlot && !owner && hook.isPlotTrusted(p);

        Boolean prevInPlot = lastInPlot.get(p.getUniqueId());
        Boolean prevOwner = lastRoleOwner.get(p.getUniqueId());
        if (prevInPlot != null && prevInPlot == inPlot
                && prevOwner != null && prevOwner == owner) return;

        lastInPlot.put(p.getUniqueId(), inPlot);
        lastRoleOwner.put(p.getUniqueId(), owner);

        applyState(p, inPlot, owner, trusted);
    }

    private void applyState(Player p, boolean inPlot, boolean owner, boolean trusted) {
        String gmPath;
        String flyPath;

        if (inPlot) {
            if (owner) {
                gmPath = "plot.auto-gamemode.inside-plot-owner";
                flyPath = "plot.auto-fly.inside-plot-owner";
            } else if (trusted) {
                gmPath = "plot.auto-gamemode.inside-plot-trusted";
                flyPath = "plot.auto-fly.inside-plot-trusted";
            } else {
                gmPath = "plot.auto-gamemode.inside-plot-visitor";
                flyPath = "plot.auto-fly.inside-plot-visitor";
            }
        } else {
            gmPath = "plot.auto-gamemode.outside-plot";
            flyPath = "plot.auto-fly.outside-plot";
        }

        String gmName = plugin.getConfig().getString(gmPath, "SURVIVAL");
        try {
            GameMode gm = GameMode.valueOf(gmName.toUpperCase(Locale.ROOT));
            if (p.getGameMode() != gm) p.setGameMode(gm);
        } catch (IllegalArgumentException ignored) {}

        boolean allowFly = plugin.getConfig().getBoolean(flyPath, false);
        if (allowFly) {
            if (!p.getAllowFlight()) p.setAllowFlight(true);
        } else {
            if (p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR) {
                if (p.isFlying()) p.setFlying(false);
                if (p.getAllowFlight()) p.setAllowFlight(false);
            }
        }
    }
}