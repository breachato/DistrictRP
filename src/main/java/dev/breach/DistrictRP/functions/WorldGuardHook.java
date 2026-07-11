package dev.breach.DistrictRP.functions;

import dev.breach.DistrictRP.DistrictRP;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class WorldGuardHook {

    private final DistrictRP plugin;
    private boolean available = false;
    private Plugin worldGuardPlugin;

    private Object worldGuardInstance;
    private Object regionContainer;

    public WorldGuardHook(DistrictRP plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        try {
            Plugin wg = Bukkit.getPluginManager().getPlugin("WorldGuard");
            if (wg == null || !wg.isEnabled()) {
                plugin.getLogger().info("[WorldGuard] Plugin non trovato o non abilitato.");
                return;
            }
            this.worldGuardPlugin = wg;

            Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Method getInstance = wgClass.getMethod("getInstance");
            this.worldGuardInstance = getInstance.invoke(null);

            Method getPlatform = wgClass.getMethod("getPlatform");
            Object platform = getPlatform.invoke(worldGuardInstance);
            Method getRegionContainer = platform.getClass().getMethod("getRegionContainer");
            this.regionContainer = getRegionContainer.invoke(platform);

            this.available = true;
            plugin.getLogger().info("[WorldGuard] Hook completato correttamente.");
        } catch (Throwable t) {
            plugin.getLogger().warning("[WorldGuard] Setup fallito: " + t.getMessage());
        }
    }

    public boolean isAvailable() { return available; }

    public String getRegionAt(Location loc) {
        if (!available || loc == null || loc.getWorld() == null) return "";
        try {
            Class<?> adapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Method adaptWorld = adapter.getMethod("adapt", org.bukkit.World.class);
            Object weWorld = adaptWorld.invoke(null, loc.getWorld());

            Method getRegions = regionContainer.getClass().getMethod("get",
                    Class.forName("com.sk89q.worldedit.world.World"));
            Object regionManager = getRegions.invoke(regionContainer, weWorld);
            if (regionManager == null) return "";

            Method adaptLoc = adapter.getMethod("adapt", Location.class);
            Object weLoc = adaptLoc.invoke(null, loc);
            Method toVec = weLoc.getClass().getMethod("toVector");
            Object vec = toVec.invoke(weLoc);
            Method toBlock = vec.getClass().getMethod("toBlockPoint");
            Object blockVec = toBlock.invoke(vec);

            Method getApplicable = regionManager.getClass().getMethod("getApplicableRegions",
                    Class.forName("com.sk89q.worldedit.math.BlockVector3"));
            Object set = getApplicable.invoke(regionManager, blockVec);

            Method size = set.getClass().getMethod("size");
            int s = (int) size.invoke(set);
            if (s == 0) return "";

            Object highest = null;
            int highestPriority = Integer.MIN_VALUE;
            for (Object region : (Iterable<?>) set) {
                Method getPriority = region.getClass().getMethod("getPriority");
                int prio = (int) getPriority.invoke(region);
                if (prio > highestPriority) {
                    highest = region;
                    highestPriority = prio;
                }
            }
            if (highest == null) return "";
            Method getId = highest.getClass().getMethod("getId");
            return (String) getId.invoke(highest);
        } catch (Throwable t) {
            return "";
        }
    }

    public boolean canBuild(Player p, Location loc) {
        if (!available) return true;
        try {
            Class<?> flagsClass = Class.forName("com.sk89q.worldguard.protection.flags.Flags");
            Object buildFlag = flagsClass.getField("BUILD").get(null);
            return testFlag(p, loc, buildFlag);
        } catch (Throwable t) {
            return true;
        }
    }

    public boolean canInteract(Player p, Location loc) {
        if (!available) return true;
        try {
            Class<?> flagsClass = Class.forName("com.sk89q.worldguard.protection.flags.Flags");
            Object interactFlag = flagsClass.getField("INTERACT").get(null);
            return testFlag(p, loc, interactFlag);
        } catch (Throwable t) {
            return true;
        }
    }

    private boolean testFlag(Player p, Location loc, Object flag) {
        try {
            Class<?> wgpClass = Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
            Method inst = wgpClass.getMethod("inst");
            Object wgp = inst.invoke(null);
            Method wrapPlayer = wgpClass.getMethod("wrapPlayer", Player.class);
            Object localPlayer = wrapPlayer.invoke(wgp, p);

            Class<?> queryClass = Class.forName("com.sk89q.worldguard.protection.regions.RegionQuery");
            Method createQuery = regionContainer.getClass().getMethod("createQuery");
            Object query = createQuery.invoke(regionContainer);

            Class<?> adapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Method adaptLoc = adapter.getMethod("adapt", Location.class);
            Object weLoc = adaptLoc.invoke(null, loc);

            Method testBuild = queryClass.getMethod("testBuild",
                    Class.forName("com.sk89q.worldedit.util.Location"),
                    Class.forName("com.sk89q.worldguard.LocalPlayer"),
                    Class.forName("com.sk89q.worldguard.protection.flags.StateFlag").arrayType());

            Object flagArray = java.lang.reflect.Array.newInstance(
                    Class.forName("com.sk89q.worldguard.protection.flags.StateFlag"), 1);
            java.lang.reflect.Array.set(flagArray, 0, flag);

            return (boolean) testBuild.invoke(query, weLoc, localPlayer, flagArray);
        } catch (Throwable t) {
            return true;
        }
    }

    public boolean runCommand(Player p, String cmd) {
        try { return Bukkit.dispatchCommand(p, cmd); }
        catch (Throwable t) { return false; }
    }

    public boolean runConsoleCommand(String cmd) {
        try { return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd); }
        catch (Throwable t) { return false; }
    }

    public List<String> listRegions(org.bukkit.World world) {
        if (!available) return Collections.emptyList();
        try {
            Class<?> adapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Method adaptWorld = adapter.getMethod("adapt", org.bukkit.World.class);
            Object weWorld = adaptWorld.invoke(null, world);

            Method getRegions = regionContainer.getClass().getMethod("get",
                    Class.forName("com.sk89q.worldedit.world.World"));
            Object regionManager = getRegions.invoke(regionContainer, weWorld);
            if (regionManager == null) return Collections.emptyList();

            Method getRegionsMap = regionManager.getClass().getMethod("getRegions");
            Object map = getRegionsMap.invoke(regionManager);
            if (map instanceof java.util.Map<?, ?> m) {
                List<String> out = new ArrayList<>();
                for (Object key : m.keySet()) out.add(String.valueOf(key));
                return out;
            }
            return Collections.emptyList();
        } catch (Throwable t) {
            return Collections.emptyList();
        }
    }
}