package dev.breach.DistrictRP.commands.roleplay.warp;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.tables.WarpsTable;
import dev.breach.DistrictRP.database.tables.WarpsTable.WarpRow;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class WarpManager {

    private final DistrictRP plugin;
    private final Map<String, Warp> warps = new LinkedHashMap<>();

    private WarpsTable table;
    private boolean useDb;

    public WarpManager(DistrictRP plugin) {
        this.plugin = plugin;
        var dbm = plugin.getDatabaseManager();
        this.table = (dbm != null && dbm.isMariaDb()) ? dbm.getTable("warps", WarpsTable.class) : null;
        this.useDb = (table != null);

        if (useDb) {
            plugin.getLogger().info("[Warps] Storage: MariaDB (con cache locale)");
            loadFromDb();
        } else {
            plugin.getLogger().info("[Warps] Storage: YAML");
            load();
        }
    }

    public void load() {
        warps.clear();
        if (useDb) {
            loadFromDb();
            return;
        }
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("warp.data");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection w = sec.getConfigurationSection(key);
            if (w == null) continue;
            String world = w.getString("world", "world");
            double x = w.getDouble("x");
            double y = w.getDouble("y");
            double z = w.getDouble("z");
            float yaw = (float) w.getDouble("yaw", 0.0);
            float pitch = (float) w.getDouble("pitch", 0.0);
            String perm = w.getString("permission", "");
            warps.put(key.toLowerCase(), new Warp(key, world, x, y, z, yaw, pitch, perm));
        }
    }

    private void loadFromDb() {
        loadAllWarps().thenAccept(list -> {
            warps.clear();
            for (Warp w : list) warps.put(w.getName().toLowerCase(), w);
            plugin.getLogger().info("[Warps] Caricati " + warps.size() + " warp dal database.");
        }).exceptionally(t -> {
            plugin.getLogger().warning("[Warps] Errore caricamento DB: " + t.getMessage());
            return null;
        });
    }

    public Warp get(String name) {
        if (name == null) return null;
        return warps.get(name.toLowerCase());
    }

    public boolean exists(String name) {
        return warps.containsKey(name.toLowerCase());
    }

    public Collection<Warp> all() {
        return warps.values();
    }

    public boolean create(String name, Location loc, String permission) {
        if (exists(name)) return false;
        Warp w = new Warp(name, loc, permission);
        warps.put(name.toLowerCase(), w);
        if (useDb) saveWarp(w);
        else saveYaml(w);
        return true;
    }

    public boolean delete(String name) {
        if (!exists(name)) return false;
        warps.remove(name.toLowerCase());
        if (useDb) {
            deleteWarp(name);
        } else {
            plugin.getConfig().set("warp.data." + name, null);
            plugin.saveConfig();
        }
        return true;
    }

    private void saveYaml(Warp w) {
        String base = "warp.data." + w.getName();
        plugin.getConfig().set(base + ".world", w.getWorld());
        plugin.getConfig().set(base + ".x", w.getX());
        plugin.getConfig().set(base + ".y", w.getY());
        plugin.getConfig().set(base + ".z", w.getZ());
        plugin.getConfig().set(base + ".yaw", w.getYaw());
        plugin.getConfig().set(base + ".pitch", w.getPitch());
        plugin.getConfig().set(base + ".permission", w.getPermission());
        plugin.saveConfig();
    }

    public void saveAll() {
        if (useDb) {
            for (Warp w : warps.values()) saveWarp(w);
        } else {
            for (Warp w : warps.values()) saveYaml(w);
        }
    }

    public boolean isUsingDatabase() { return useDb; }

    public CompletableFuture<List<Warp>> loadAllWarps() {
        if (table == null) return CompletableFuture.completedFuture(new ArrayList<>());
        return table.all().thenApply(rows -> {
            List<Warp> out = new ArrayList<>();
            for (WarpRow r : rows) out.add(toWarp(r));
            return out;
        });
    }

    public CompletableFuture<Boolean> saveWarp(Warp w) {
        if (table == null) return CompletableFuture.completedFuture(false);
        return table.upsert(toRow(w));
    }

    public CompletableFuture<Boolean> deleteWarp(String name) {
        if (table == null) return CompletableFuture.completedFuture(false);
        return table.delete(name);
    }

    private Warp toWarp(WarpRow r) {
        return new Warp(r.name, r.world, r.x, r.y, r.z, r.yaw, r.pitch, r.permission);
    }

    private WarpRow toRow(Warp w) {
        WarpRow r = new WarpRow();
        r.name = w.getName();
        r.world = w.getWorld();
        r.x = w.getX();
        r.y = w.getY();
        r.z = w.getZ();
        r.yaw = w.getYaw();
        r.pitch = w.getPitch();
        r.permission = w.getPermission();
        return r;
    }
}