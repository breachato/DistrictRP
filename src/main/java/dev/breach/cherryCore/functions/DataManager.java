package dev.breach.cherryCore.functions;

import dev.breach.cherryCore.CherryCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataManager {

    private final CherryCore plugin;

    private File homesFile, warpsFile, spawnFile, permsFile, recFile, elevatorsFile, mondiFile;
    private FileConfiguration homes, warps, spawn, perms, rec, elevators, mondi;
    private File staffFile;
    private FileConfiguration staff;

    public DataManager(CherryCore plugin) {
        this.plugin = plugin;
        plugin.getDataFolder().mkdirs();

        homesFile     = load("homes.yml");
        warpsFile     = load("warps.yml");
        spawnFile     = load("spawn.yml");
        permsFile     = load("perms.yml");
        recFile       = load("rec.yml");
        elevatorsFile = load("elevators.yml");
        mondiFile     = load("mondi.yml");
        staffFile = load("staff.yml");

        staff = YamlConfiguration.loadConfiguration(staffFile);
        homes     = YamlConfiguration.loadConfiguration(homesFile);
        warps     = YamlConfiguration.loadConfiguration(warpsFile);
        spawn     = YamlConfiguration.loadConfiguration(spawnFile);
        perms     = YamlConfiguration.loadConfiguration(permsFile);
        rec       = YamlConfiguration.loadConfiguration(recFile);
        elevators = YamlConfiguration.loadConfiguration(elevatorsFile);
        mondi     = YamlConfiguration.loadConfiguration(mondiFile);
    }

    private File load(String name) {
        File f = new File(plugin.getDataFolder(), name);
        if (!f.exists()) {
            try { f.createNewFile(); } catch (IOException ignored) {}
        }
        return f;
    }

    public void saveAll() {
        save(homes, homesFile);
        save(warps, warpsFile);
        save(spawn, spawnFile);
        save(perms, permsFile);
        save(rec,   recFile);
        save(elevators, elevatorsFile);
        save(mondi, mondiFile);
        save(staff, staffFile);
    }

    private void save(FileConfiguration c, File f) {
        try { c.save(f); } catch (IOException e) { e.printStackTrace(); }
    }

    // ============================================================
    // HELPERS LOCATION
    // ============================================================
    public static Map<String, Object> locToMap(Location l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("world", l.getWorld().getName());
        m.put("x", l.getX());
        m.put("y", l.getY());
        m.put("z", l.getZ());
        m.put("yaw", l.getYaw());
        m.put("pitch", l.getPitch());
        return m;
    }

    public static Location mapToLoc(Map<String, Object> m) {
        if (m == null) return null;
        World w = Bukkit.getWorld((String) m.get("world"));
        if (w == null) return null;
        double x = ((Number) m.get("x")).doubleValue();
        double y = ((Number) m.get("y")).doubleValue();
        double z = ((Number) m.get("z")).doubleValue();
        float yaw   = ((Number) m.getOrDefault("yaw",   0)).floatValue();
        float pitch = ((Number) m.getOrDefault("pitch", 0)).floatValue();
        return new Location(w, x, y, z, yaw, pitch);
    }

    public static Location sectionToLoc(ConfigurationSection sec) {
        if (sec == null) return null;
        World w = Bukkit.getWorld(sec.getString("world", ""));
        if (w == null) return null;
        return new Location(w,
                sec.getDouble("x"),
                sec.getDouble("y"),
                sec.getDouble("z"),
                (float) sec.getDouble("yaw", 0),
                (float) sec.getDouble("pitch", 0));
    }

    // ============================================================
    // GETTERS CONFIG
    // ============================================================
    public FileConfiguration homes()     { return homes; }
    public FileConfiguration warps()     { return warps; }
    public FileConfiguration spawn()     { return spawn; }
    public FileConfiguration perms()     { return perms; }
    public FileConfiguration rec()       { return rec; }
    public FileConfiguration elevators() { return elevators; }
    public FileConfiguration mondi()     { return mondi; }

    public void saveHomes()     { save(homes, homesFile); }
    public void saveWarps()     { save(warps, warpsFile); }
    public void saveSpawn()     { save(spawn, spawnFile); }
    public void savePerms()     { save(perms, permsFile); }
    public void saveRec()       { save(rec, recFile); }
    public void saveElevators() { save(elevators, elevatorsFile); }
    public void saveMondi()     { save(mondi, mondiFile); }

    public FileConfiguration staff() { return staff; }
    public void saveStaff() { save(staff, staffFile); }

    // === VANISH ===
    public boolean isVanished(UUID uuid) {
        return staff.getBoolean("vanish." + uuid, false);
    }
    public void setVanished(UUID uuid, boolean v) {
        if (v) staff.set("vanish." + uuid, true);
        else   staff.set("vanish." + uuid, null);
        saveStaff();
    }
    public Set<String> getAllVanished() {
        ConfigurationSection s = staff.getConfigurationSection("vanish");
        return s == null ? new HashSet<>() : s.getKeys(false);
    }

    // === STAFF MODE ===
    public boolean isStaffMode(UUID uuid) {
        return staff.getBoolean("staffmode." + uuid + ".active", false);
    }
    public void saveStaffSnapshot(UUID uuid, Map<String, Object> snap) {
        staff.set("staffmode." + uuid, snap);
        saveStaff();
    }
    public Map<String, Object> getStaffSnapshot(UUID uuid) {
        ConfigurationSection s = staff.getConfigurationSection("staffmode." + uuid);
        if (s == null) return null;
        return s.getValues(true);
    }
    public void clearStaffSnapshot(UUID uuid) {
        staff.set("staffmode." + uuid, null);
        saveStaff();
    }

    // === DISGUISE ===
    public String getDisguise(UUID uuid) {
        return staff.getString("disguise." + uuid);
    }
    public void setDisguise(UUID uuid, String name) {
        if (name == null) staff.set("disguise." + uuid, null);
        else staff.set("disguise." + uuid, name);
        saveStaff();
    }

    // ============================================================
    // HOMES
    // ============================================================
    public void setHome(UUID uuid, String name, Location loc) {
        homes.set(uuid + "." + name, locToMap(loc));
        saveHomes();
    }
    public Location getHome(UUID uuid, String name) {
        return sectionToLoc(homes.getConfigurationSection(uuid + "." + name));
    }
    public void delHome(UUID uuid, String name) {
        homes.set(uuid + "." + name, null);
        saveHomes();
    }
    public Set<String> listHomes(UUID uuid) {
        ConfigurationSection s = homes.getConfigurationSection(uuid.toString());
        return s == null ? Collections.emptySet() : s.getKeys(false);
    }

    // ============================================================
    // WARPS
    // ============================================================
    public void setWarp(String name, Location loc, String creator) {
        warps.set(name + ".loc", locToMap(loc));
        warps.set(name + ".creator", creator);
        saveWarps();
    }
    public Location getWarp(String name) {
        return sectionToLoc(warps.getConfigurationSection(name + ".loc"));
    }
    public void delWarp(String name) {
        warps.set(name, null);
        saveWarps();
    }
    public Set<String> listWarps() {
        return warps.getKeys(false);
    }

    // ============================================================
    // SPAWN
    // ============================================================
    public void setSpawn(Location l) { spawn.set("spawn", locToMap(l)); saveSpawn(); }
    public Location getSpawn()       { return sectionToLoc(spawn.getConfigurationSection("spawn")); }

    // ============================================================
    // MONDI - WHITELIST / BLACKLIST
    // ============================================================
    public List<String> getWhitelist(String world) {
        return mondi.getStringList("worlds." + world + ".whitelist");
    }
    public void addWhitelist(String world, UUID uuid) {
        List<String> l = new ArrayList<>(getWhitelist(world));
        String s = uuid.toString();
        if (!l.contains(s)) {
            l.add(s);
            mondi.set("worlds." + world + ".whitelist", l);
            saveMondi();
        }
    }
    public void removeWhitelist(String world, UUID uuid) {
        List<String> l = new ArrayList<>(getWhitelist(world));
        if (l.remove(uuid.toString())) {
            mondi.set("worlds." + world + ".whitelist", l);
            saveMondi();
        }
    }

    public List<String> getBlacklist(String world) {
        return mondi.getStringList("worlds." + world + ".blacklist");
    }
    public void addBlacklist(String world, UUID uuid) {
        List<String> l = new ArrayList<>(getBlacklist(world));
        String s = uuid.toString();
        if (!l.contains(s)) {
            l.add(s);
            mondi.set("worlds." + world + ".blacklist", l);
            saveMondi();
        }
    }
    public void removeBlacklist(String world, UUID uuid) {
        List<String> l = new ArrayList<>(getBlacklist(world));
        if (l.remove(uuid.toString())) {
            mondi.set("worlds." + world + ".blacklist", l);
            saveMondi();
        }
    }

    public boolean isWhitelistEnabled(String world) {
        return !getWhitelist(world).isEmpty();
    }

    public boolean isBlacklisted(String world, UUID uuid) {
        return getBlacklist(world).contains(uuid.toString());
    }

    public boolean isWhitelisted(String world, UUID uuid) {
        return getWhitelist(world).contains(uuid.toString());
    }

    public Set<String> listMondi() {
        ConfigurationSection s = mondi.getConfigurationSection("worlds");
        return s == null ? new HashSet<>() : s.getKeys(false);
    }
}