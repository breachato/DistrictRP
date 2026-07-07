package dev.breach.DistrictRP.commands.roleplay.plot;

import dev.breach.DistrictRP.DistrictRP;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlotSquaredHook {

    private final DistrictRP plugin;
    private boolean available = false;
    private Plugin plotSquaredPlugin;

    private Class<?> classBukkitUtil;
    private Class<?> classPlotPlayer;
    private Class<?> classPlot;
    private Class<?> classLocation;
    private Class<?> classPlotAreaManager;

    private Method mAdapt;
    private Method mGetLocation;
    private Method mGetCurrentPlot;
    private Method mGetOwner;
    private Method mGetTrusted;
    private Method mGetMembers;
    private Method mGetDenied;
    private Method mGetId;
    private Method mGetIdX;
    private Method mGetIdY;
    private Method mHasOwner;
    private Method mGetPlots;
    private Method mPlotGetOwners;

    public PlotSquaredHook(DistrictRP plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        try {
            Plugin p = Bukkit.getPluginManager().getPlugin("PlotSquared");
            if (p == null || !p.isEnabled()) {
                plugin.getLogger().info("[PlotSquared] Plugin non trovato o non abilitato.");
                return;
            }
            this.plotSquaredPlugin = p;

            classBukkitUtil     = tryLoad("com.plotsquared.bukkit.util.BukkitUtil");
            classPlotPlayer     = tryLoad("com.plotsquared.core.player.PlotPlayer");
            classPlot           = tryLoad("com.plotsquared.core.plot.Plot");
            classLocation       = tryLoad("com.plotsquared.core.location.Location");
            classPlotAreaManager= tryLoad("com.plotsquared.core.plot.world.PlotAreaManager");

            if (classPlotPlayer == null || classPlot == null) {
                plugin.getLogger().warning("[PlotSquared] Classi core non trovate, hook fallito.");
                return;
            }

            if (classBukkitUtil != null) {
                try {
                    mAdapt = classBukkitUtil.getMethod("adapt", Player.class);
                } catch (NoSuchMethodException ignored) {}
            }

            try { mGetLocation      = classPlotPlayer.getMethod("getLocation"); } catch (Throwable ignored) {}
            try { mGetCurrentPlot   = classPlotPlayer.getMethod("getCurrentPlot"); } catch (Throwable ignored) {}
            try { mGetOwner         = classPlot.getMethod("getOwnerAbs"); } catch (Throwable ignored) {}
            try { mGetTrusted       = classPlot.getMethod("getTrusted"); } catch (Throwable ignored) {}
            try { mGetMembers       = classPlot.getMethod("getMembers"); } catch (Throwable ignored) {}
            try { mGetDenied        = classPlot.getMethod("getDenied"); } catch (Throwable ignored) {}
            try { mGetId            = classPlot.getMethod("getId"); } catch (Throwable ignored) {}
            try { mHasOwner         = classPlot.getMethod("hasOwner"); } catch (Throwable ignored) {}
            try { mGetPlots         = classPlotPlayer.getMethod("getPlots"); } catch (Throwable ignored) {}
            try { mPlotGetOwners    = classPlot.getMethod("getOwners"); } catch (Throwable ignored) {}

            available = true;
            plugin.getLogger().info("[PlotSquared] Hook completato correttamente.");
        } catch (Throwable t) {
            plugin.getLogger().warning("[PlotSquared] Setup fallito: " + t.getMessage());
        }
    }

    private Class<?> tryLoad(String name) {
        try { return Class.forName(name); }
        catch (Throwable t) { return null; }
    }

    public boolean isAvailable() { return available; }

    private Object toPlotPlayer(Player p) {
        if (!available || mAdapt == null) return null;
        try { return mAdapt.invoke(null, p); }
        catch (Throwable t) { return null; }
    }

    private Object getCurrentPlotObject(Player p) {
        Object pp = toPlotPlayer(p);
        if (pp == null || mGetCurrentPlot == null) return null;
        try { return mGetCurrentPlot.invoke(pp); }
        catch (Throwable t) { return null; }
    }

    public boolean isInPlot(Player p) {
        return getCurrentPlotObject(p) != null;
    }

    public String getCurrentPlotId(Player p) {
        Object plot = getCurrentPlotObject(p);
        if (plot == null || mGetId == null) return "";
        try {
            Object id = mGetId.invoke(plot);
            return id == null ? "" : id.toString();
        } catch (Throwable t) { return ""; }
    }

    public UUID getCurrentPlotOwner(Player p) {
        Object plot = getCurrentPlotObject(p);
        if (plot == null || mGetOwner == null) return null;
        try {
            Object owner = mGetOwner.invoke(plot);
            return (owner instanceof UUID) ? (UUID) owner : null;
        } catch (Throwable t) { return null; }
    }

    public String getCurrentPlotOwnerName(Player p) {
        UUID owner = getCurrentPlotOwner(p);
        if (owner == null) return "";
        String name = Bukkit.getOfflinePlayer(owner).getName();
        return name == null ? "" : name;
    }

    public boolean isPlotOwner(Player p) {
        UUID owner = getCurrentPlotOwner(p);
        return owner != null && owner.equals(p.getUniqueId());
    }

    @SuppressWarnings("unchecked")
    public boolean isPlotTrusted(Player p) {
        Object plot = getCurrentPlotObject(p);
        if (plot == null) return false;
        try {
            Set<UUID> trusted = mGetTrusted != null ? (Set<UUID>) mGetTrusted.invoke(plot) : Collections.emptySet();
            if (trusted != null && trusted.contains(p.getUniqueId())) return true;
            Set<UUID> members = mGetMembers != null ? (Set<UUID>) mGetMembers.invoke(plot) : Collections.emptySet();
            return members != null && members.contains(p.getUniqueId());
        } catch (Throwable t) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public boolean isPlotDenied(Player p) {
        Object plot = getCurrentPlotObject(p);
        if (plot == null || mGetDenied == null) return false;
        try {
            Set<UUID> denied = (Set<UUID>) mGetDenied.invoke(plot);
            return denied != null && denied.contains(p.getUniqueId());
        } catch (Throwable t) { return false; }
    }

    @SuppressWarnings("unchecked")
    public int getOwnedPlotsCount(Player p) {
        Object pp = toPlotPlayer(p);
        if (pp == null || mGetPlots == null) return 0;
        try {
            Object plots = mGetPlots.invoke(pp);
            if (plots instanceof Collection) return ((Collection<?>) plots).size();
            return 0;
        } catch (Throwable t) { return 0; }
    }

    public boolean isInPlotWorld(Player p, String plotWorldName) {
        if (p == null || p.getWorld() == null) return false;
        return p.getWorld().getName().equalsIgnoreCase(plotWorldName);
    }

    public boolean runPlotSquaredCommand(Player p, String command) {
        try {
            return Bukkit.dispatchCommand(p, command);
        } catch (Throwable t) {
            return false;
        }
    }
}