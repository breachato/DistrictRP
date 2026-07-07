package dev.breach.DistrictRP.commands.roleplay.warp;

import dev.breach.DistrictRP.DistrictRP;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class WarpManager {

    private final DistrictRP plugin;
    private final Map<String, Warp> warps = new LinkedHashMap<>();

    public WarpManager(DistrictRP plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        warps.clear();
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
        save(w);
        return true;
    }

    public boolean delete(String name) {
        if (!exists(name)) return false;
        warps.remove(name.toLowerCase());
        plugin.getConfig().set("warp.data." + name, null);
        plugin.saveConfig();
        return true;
    }

    private void save(Warp w) {
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
        for (Warp w : warps.values()) save(w);
    }
}