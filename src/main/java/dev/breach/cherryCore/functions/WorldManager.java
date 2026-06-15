package dev.breach.cherryCore.functions;

import dev.breach.cherryCore.CherryCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WorldManager {

    private final CherryCore plugin;
    private final Object mvWorldManager;
    private final boolean mvAvailable;

    public WorldManager(CherryCore plugin) {
        this.plugin = plugin;
        Object mvwm = null;
        boolean ok = false;
        try {
            Plugin mv = Bukkit.getPluginManager().getPlugin("Multiverse-Core");
            if (mv != null && mv.isEnabled()) {
                Method getMVWM = mv.getClass().getMethod("getMVWorldManager");
                mvwm = getMVWM.invoke(mv);
                ok = (mvwm != null);
                if (ok) plugin.getLogger().info("Multiverse-Core hookato con successo.");
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Hook Multiverse fallito: " + t.getMessage());
        }
        this.mvWorldManager = mvwm;
        this.mvAvailable = ok;
        if (!ok) plugin.getLogger().info("Multiverse non disponibile → uso Bukkit standalone.");
    }

    public boolean isReady() { return true; }
    public boolean isMultiverseHooked() { return mvAvailable; }
    
    public boolean worldExists(String name) {
        if (name == null) return false;
        if (mvAvailable) {
            Boolean res = (Boolean) reflectInvoke(mvWorldManager, "isMVWorld",
                    new Class<?>[]{String.class}, name);
            if (Boolean.TRUE.equals(res)) return true;
        }
        if (Bukkit.getWorld(name) != null) return true;
        File f = new File(Bukkit.getWorldContainer(), name);
        return f.isDirectory() && new File(f, "level.dat").exists();
    }

    public boolean importWorld(String folderName, World.Environment env) {
        if (folderName == null || folderName.isEmpty()) return false;

        File folder = new File(Bukkit.getWorldContainer(), folderName);
        if (!folder.exists() || !folder.isDirectory()) {
            plugin.getLogger().warning("importWorld: cartella " + folderName + " non esiste.");
            return false;
        }

        if (mvAvailable) {
            try {
                Boolean exists = (Boolean) reflectInvoke(mvWorldManager, "isMVWorld",
                        new Class<?>[]{String.class}, folderName);
                if (Boolean.TRUE.equals(exists)) return true;

                Method addWorld = mvWorldManager.getClass().getMethod(
                        "addWorld",
                        String.class,
                        World.Environment.class,
                        String.class,
                        org.bukkit.WorldType.class,
                        Boolean.class,
                        String.class
                );
                Object res = addWorld.invoke(mvWorldManager, folderName, env, null, null, true, null);
                if (res instanceof Boolean && (Boolean) res) return true;
            } catch (Throwable t) {
                plugin.getLogger().warning("MV importWorld fallita, fallback Bukkit: " + t.getMessage());
            }
        }

        try {
            if (Bukkit.getWorld(folderName) != null) return true;
            WorldCreator wc = new WorldCreator(folderName).environment(env);
            return Bukkit.createWorld(wc) != null;
        } catch (Throwable t) {
            plugin.getLogger().warning("Bukkit createWorld fallita: " + t.getMessage());
            return false;
        }
    }

    public boolean removeWorld(String name) {
        if (name == null) return false;

        if (mvAvailable) {
            try {
                Boolean exists = (Boolean) reflectInvoke(mvWorldManager, "isMVWorld",
                        new Class<?>[]{String.class}, name);
                if (Boolean.TRUE.equals(exists)) {
                    Boolean res = (Boolean) reflectInvoke(mvWorldManager, "unloadWorld",
                            new Class<?>[]{String.class}, name);
                    if (Boolean.TRUE.equals(res)) return true;
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("MV unloadWorld fallita, fallback Bukkit: " + t.getMessage());
            }
        }

        try {
            World w = Bukkit.getWorld(name);
            if (w == null) return true;

            World main = Bukkit.getWorlds().get(0);
            if (!w.equals(main)) {
                for (Player p : w.getPlayers()) {
                    p.teleport(main.getSpawnLocation());
                }
            }
            return Bukkit.unloadWorld(w, true);
        } catch (Throwable t) {
            plugin.getLogger().warning("Bukkit unloadWorld fallita: " + t.getMessage());
            return false;
        }
    }

    public boolean teleport(Player p, String worldName) {
        if (p == null || worldName == null) return false;

        if (mvAvailable) {
            try {
                Method getMVWorld = mvWorldManager.getClass().getMethod("getMVWorld", String.class);
                Object mvw = getMVWorld.invoke(mvWorldManager, worldName);
                if (mvw != null) {
                    Method getSpawn = mvw.getClass().getMethod("getSpawnLocation");
                    Object loc = getSpawn.invoke(mvw);
                    if (loc instanceof Location) {
                        return p.teleport((Location) loc);
                    }
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("MV teleport fallita, fallback Bukkit: " + t.getMessage());
            }
        }

        try {
            World w = Bukkit.getWorld(worldName);
            if (w == null) {
                File folder = new File(Bukkit.getWorldContainer(), worldName);
                if (folder.isDirectory() && new File(folder, "level.dat").exists()) {
                    w = Bukkit.createWorld(new WorldCreator(worldName));
                }
            }
            if (w == null) return false;
            return p.teleport(w.getSpawnLocation());
        } catch (Throwable t) {
            plugin.getLogger().warning("Bukkit teleport fallita: " + t.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> listWorlds() {
        List<String> result = new ArrayList<>();

        if (mvAvailable) {
            try {
                Method getMVWorlds = mvWorldManager.getClass().getMethod("getMVWorlds");
                Object res = getMVWorlds.invoke(mvWorldManager);
                if (res instanceof Collection) {
                    for (Object w : (Collection<Object>) res) {
                        try {
                            Method getName = w.getClass().getMethod("getName");
                            Object n = getName.invoke(w);
                            if (n instanceof String) result.add((String) n);
                        } catch (Throwable ignored) {}
                    }
                    if (!result.isEmpty()) return result;
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("MV listWorlds fallita, fallback Bukkit: " + t.getMessage());
            }
        }

        for (World w : Bukkit.getWorlds()) {
            result.add(w.getName());
        }
        return result;
    }
    
    private Object reflectInvoke(Object target, String methodName, Class<?>[] paramTypes, Object... args) {
        try {
            Method m = target.getClass().getMethod(methodName, paramTypes);
            return m.invoke(target, args);
        } catch (Throwable t) {
            return null;
        }
    }
}
