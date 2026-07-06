package dev.breach.DistrictRP.commands.utils;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class SpawnCommands {

    private final DistrictRP plugin;
    private final File file;
    private FileConfiguration config;

    public SpawnCommands(DistrictRP plugin) {
        this.plugin = plugin;
        File dir = new File(plugin.getDataFolder(), "roleplay");
        if (!dir.exists()) dir.mkdirs();
        this.file = new File(dir, "spawns.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public CommandExecutor setSpawn() {
        return (sender, command, label, args) -> {
            if (!(sender instanceof Player player)) {
                MessageUtils.sendMsg(sender, "general.only-player");
                return true;
            }
            String perm = plugin.getConfig().getString("spawn.set-permission", "DistrictRP.setspawn");
            if (!player.hasPermission(perm)) {
                MessageUtils.sendMsg(sender, "general.no-permission");
                return true;
            }

            String worldName = args.length >= 1 ? args[0] : player.getWorld().getName();
            Location loc = player.getLocation();

            saveSpawn(worldName, loc);
            player.getWorld().setSpawnLocation(loc);

            MessageUtils.sendMsg(sender, "spawn.set", "world", worldName);
            return true;
        };
    }

    public CommandExecutor spawn() {
        return (sender, command, label, args) -> {
            if (!(sender instanceof Player player)) {
                MessageUtils.sendMsg(sender, "general.only-player");
                return true;
            }
            String perm = plugin.getConfig().getString("spawn.permission", "DistrictRP.spawn");
            if (!player.hasPermission(perm)) {
                MessageUtils.sendMsg(sender, "general.no-permission");
                return true;
            }

            String worldName;
            if (args.length >= 1) {
                worldName = args[0];
            } else {
                worldName = plugin.getConfig().getString("spawn.default-world", player.getWorld().getName());
            }

            Location loc = loadSpawn(worldName);
            if (loc == null) {
                World w = Bukkit.getWorld(worldName);
                if (w == null) {
                    MessageUtils.sendMsg(sender, "spawn.world-not-found", "world", worldName);
                    return true;
                }
                loc = w.getSpawnLocation();
            }

            if (plugin.back != null) {
                plugin.back.put(player.getUniqueId(), player.getLocation());
            }
            player.teleport(loc);
            MessageUtils.sendMsg(sender, "spawn.teleported", "world", worldName);
            return true;
        };
    }

    private void saveSpawn(String worldName, Location loc) {
        FileConfiguration newConfig = new YamlConfiguration();
        ConfigurationSection spawnsSec = newConfig.createSection("spawns");
        if (config.isConfigurationSection("spawns")) {
            for (String key : config.getConfigurationSection("spawns").getKeys(false)) {
                ConfigurationSection s = config.getConfigurationSection("spawns." + key);
                if (s == null) continue;
                ConfigurationSection ns = spawnsSec.createSection(key);
                ns.set("world", s.getString("world"));
                ns.set("x", s.getDouble("x"));
                ns.set("y", s.getDouble("y"));
                ns.set("z", s.getDouble("z"));
                ns.set("yaw", s.getDouble("yaw"));
                ns.set("pitch", s.getDouble("pitch"));
            }
        }
        ConfigurationSection ws = spawnsSec.createSection(worldName);
        ws.set("world", loc.getWorld().getName());
        ws.set("x", loc.getX());
        ws.set("y", loc.getY());
        ws.set("z", loc.getZ());
        ws.set("yaw", (double) loc.getYaw());
        ws.set("pitch", (double) loc.getPitch());
        this.config = newConfig;
        try { config.save(file); }
        catch (IOException e) { plugin.getLogger().warning("Errore salvataggio spawns.yml: " + e.getMessage()); }
    }

    private Location loadSpawn(String worldName) {
        if (!config.isConfigurationSection("spawns." + worldName)) return null;
        String w = config.getString("spawns." + worldName + ".world", worldName);
        World world = Bukkit.getWorld(w);
        if (world == null) return null;
        double x = config.getDouble("spawns." + worldName + ".x");
        double y = config.getDouble("spawns." + worldName + ".y");
        double z = config.getDouble("spawns." + worldName + ".z");
        float yaw = (float) config.getDouble("spawns." + worldName + ".yaw");
        float pitch = (float) config.getDouble("spawns." + worldName + ".pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }
}