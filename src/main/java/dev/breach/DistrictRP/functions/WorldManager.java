package dev.breach.DistrictRP.functions;

import dev.breach.DistrictRP.DistrictRP;
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

    private final DistrictRP plugin;
    private final Object mvWorldManager;
    private final boolean mvAvailable;
    private final int mvMajorVersion;

    public WorldManager(DistrictRP plugin) {
        this.plugin = plugin;
        Object mvwm = null;
        boolean ok = false;
        int major = 0;

        try {
            Plugin mv = Bukkit.getPluginManager().getPlugin("Multiverse-Core");
            if (mv != null && mv.isEnabled()) {
                String ver = mv.getDescription().getVersion();
                if (ver != null && !ver.isEmpty()) {
                    try {
                        String num = ver.split("[.-]")[0];
                        major = Integer.parseInt(num);
                    } catch (Exception ignored) {}
                }

                if (major >= 5) {
                    try {
                        Class<?> mvCoreClass = mv.getClass();
                        Method getService = mvCoreClass.getMethod("getService", Class.class);
                        Class<?> wmClass = Class.forName("org.mvplugins.multiverse.core.world.WorldManager");
                        mvwm = getService.invoke(mv, wmClass);
                        ok = (mvwm != null);
                        if (ok) plugin.getLogger().info("Multiverse-Core 5.x hookato correttamente.");
                    } catch (Throwable t5) {
                        plugin.getLogger().warning("Hook MV 5.x fallito: " + t5.getMessage());
                    }
                } else {
                    try {
                        Method getMVWM = mv.getClass().getMethod("getMVWorldManager");
                        mvwm = getMVWM.invoke(mv);
                        ok = (mvwm != null);
                        if (ok) plugin.getLogger().info("Multiverse-Core 4.x hookato correttamente.");
                    } catch (Throwable t4) {
                        plugin.getLogger().warning("Hook MV 4.x fallito: " + t4.getMessage());
                    }
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Hook Multiverse fallito globalmente: " + t.getMessage());
        }
        this.mvWorldManager = mvwm;
        this.mvAvailable = ok;
        this.mvMajorVersion = major;
        if (!ok) plugin.getLogger().info("Multiverse non disponibile → uso Bukkit standalone.");
    }

    public boolean isReady() { return true; }
    public boolean isMultiverseHooked() { return mvAvailable; }
    public int getMvMajorVersion() { return mvMajorVersion; }

    public boolean worldExists(String name) {
        if (name == null) return false;

        if (mvAvailable) {
            try {
                if (mvMajorVersion >= 5) {
                    Method isLoaded = mvWorldManager.getClass().getMethod("isLoadedWorld", String.class);
                    Object res = isLoaded.invoke(mvWorldManager, name);
                    if (Boolean.TRUE.equals(res)) return true;
                } else {
                    Method isMV = mvWorldManager.getClass().getMethod("isMVWorld", String.class);
                    Object res = isMV.invoke(mvWorldManager, name);
                    if (Boolean.TRUE.equals(res)) return true;
                }
            } catch (Throwable ignored) {}
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
                if (worldExists(folderName)) return true;

                if (mvMajorVersion >= 5) {
                    return importWorldMV5(folderName, env);
                } else {
                    return importWorldMV4(folderName, env);
                }
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

    private boolean importWorldMV4(String folderName, World.Environment env) throws Exception {
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
        return res instanceof Boolean && (Boolean) res;
    }

    private boolean importWorldMV5(String folderName, World.Environment env) {
        try {
            Class<?> importOpts = Class.forName("org.mvplugins.multiverse.core.world.options.ImportWorldOptions");
            Method worldName = importOpts.getMethod("worldName", String.class);
            Object opts = worldName.invoke(null, folderName);
            Method envMethod = opts.getClass().getMethod("environment", World.Environment.class);
            opts = envMethod.invoke(opts, env);

            Method imp = mvWorldManager.getClass().getMethod("importWorld", importOpts);
            Object result = imp.invoke(mvWorldManager, opts);

            Method isSuccess = result.getClass().getMethod("isSuccess");
            Object success = isSuccess.invoke(result);
            return Boolean.TRUE.equals(success);
        } catch (Throwable t) {
            plugin.getLogger().warning("MV5 importWorld fallita: " + t.getMessage());
            return false;
        }
    }

    public boolean removeWorld(String name) {
        if (name == null) return false;

        if (mvAvailable) {
            try {
                if (mvMajorVersion >= 5) {
                    Method getWorld = mvWorldManager.getClass().getMethod("getLoadedWorld", String.class);
                    Object worldOpt = getWorld.invoke(mvWorldManager, name);
                    if (worldOpt != null) {
                        Method unload = mvWorldManager.getClass().getMethod("unloadWorld", worldOpt.getClass());
                        Object res = unload.invoke(mvWorldManager, worldOpt);
                        Method isSuccess = res.getClass().getMethod("isSuccess");
                        if (Boolean.TRUE.equals(isSuccess.invoke(res))) return true;
                    }
                } else {
                    Method isMV = mvWorldManager.getClass().getMethod("isMVWorld", String.class);
                    Object exists = isMV.invoke(mvWorldManager, name);
                    if (Boolean.TRUE.equals(exists)) {
                        Method unload = mvWorldManager.getClass().getMethod("unloadWorld", String.class);
                        Object res = unload.invoke(mvWorldManager, name);
                        if (Boolean.TRUE.equals(res)) return true;
                    }
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
                for (Player p : w.getPlayers()) p.teleport(main.getSpawnLocation());
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
                if (mvMajorVersion >= 5) {
                    Method getWorld = mvWorldManager.getClass().getMethod("getLoadedWorld", String.class);
                    Object worldOpt = getWorld.invoke(mvWorldManager, worldName);
                    if (worldOpt != null) {
                        Method getOrNull = worldOpt.getClass().getMethod("getOrNull");
                        Object mvw = getOrNull.invoke(worldOpt);
                        if (mvw != null) {
                            Method getSpawn = mvw.getClass().getMethod("getSpawnLocation");
                            Object loc = getSpawn.invoke(mvw);
                            if (loc instanceof Location) return p.teleport((Location) loc);
                        }
                    }
                } else {
                    Method getMVWorld = mvWorldManager.getClass().getMethod("getMVWorld", String.class);
                    Object mvw = getMVWorld.invoke(mvWorldManager, worldName);
                    if (mvw != null) {
                        Method getSpawn = mvw.getClass().getMethod("getSpawnLocation");
                        Object loc = getSpawn.invoke(mvw);
                        if (loc instanceof Location) return p.teleport((Location) loc);
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
                if (mvMajorVersion >= 5) {
                    Method getWorlds = mvWorldManager.getClass().getMethod("getLoadedWorlds");
                    Object res = getWorlds.invoke(mvWorldManager);
                    if (res instanceof Collection) {
                        for (Object mvw : (Collection<Object>) res) {
                            try {
                                Method getName = mvw.getClass().getMethod("getName");
                                Object n = getName.invoke(mvw);
                                if (n instanceof String) result.add((String) n);
                            } catch (Throwable ignored) {}
                        }
                        if (!result.isEmpty()) return result;
                    }
                } else {
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
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("MV listWorlds fallita, fallback Bukkit: " + t.getMessage());
            }
        }

        for (World w : Bukkit.getWorlds()) result.add(w.getName());
        return result;
    }
}