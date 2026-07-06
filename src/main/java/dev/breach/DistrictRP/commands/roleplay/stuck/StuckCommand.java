package dev.breach.DistrictRP.commands.roleplay.stuck;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StuckCommand implements CommandExecutor, TabCompleter, Listener {

    private static final String ADMIN_PERM = "DistrictRP.stuck.admin";

    private final DistrictRP plugin;
    private final Map<UUID, StuckSession> sessions = new HashMap<>();
    private final Map<String, Location> safes = new LinkedHashMap<>();
    private final File file;
    private FileConfiguration config;

    public StuckCommand(DistrictRP plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        File dir = new File(plugin.getDataFolder(), "roleplay");
        if (!dir.exists()) dir.mkdirs();
        this.file = new File(dir, "stucksafes.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        loadSafes();
    }

    private void loadSafes() {
        safes.clear();
        ConfigurationSection sec = config.getConfigurationSection("safes");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            String base = "safes." + key + ".";
            String worldName = config.getString(base + "world");
            if (worldName == null) continue;
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;
            double x = config.getDouble(base + "x");
            double y = config.getDouble(base + "y");
            double z = config.getDouble(base + "z");
            float yaw = (float) config.getDouble(base + "yaw", 0);
            float pitch = (float) config.getDouble(base + "pitch", 0);
            safes.put(key.toLowerCase(), new Location(world, x, y, z, yaw, pitch));
        }
    }

    private void saveSafes() {
        FileConfiguration newConfig = new YamlConfiguration();
        ConfigurationSection sec = newConfig.createSection("safes");
        for (Map.Entry<String, Location> e : safes.entrySet()) {
            ConfigurationSection s = sec.createSection(e.getKey());
            Location loc = e.getValue();
            s.set("world", loc.getWorld().getName());
            s.set("x", loc.getX());
            s.set("y", loc.getY());
            s.set("z", loc.getZ());
            s.set("yaw", loc.getYaw());
            s.set("pitch", loc.getPitch());
        }
        this.config = newConfig;
        try { config.save(file); }
        catch (IOException e) { plugin.getLogger().warning("Errore salvataggio stucksafes.yml: " + e.getMessage()); }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendMsg(sender, "general.only-player");
            return true;
        }

        if (args.length > 0 && player.hasPermission(ADMIN_PERM)) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "aggiungisafe" -> {
                    if (args.length < 2) {
                        MessageUtils.sendMsg(player, "stuck.usage-aggiungisafe");
                        return true;
                    }
                    handleAdd(player, args[1]);
                    return true;
                }
                case "rimuovisafe" -> {
                    if (args.length < 2) {
                        MessageUtils.sendMsg(player, "stuck.usage-rimuovisafe");
                        return true;
                    }
                    handleRemove(player, args[1]);
                    return true;
                }
                case "lista" -> {
                    handleList(player);
                    return true;
                }
                case "visita" -> {
                    if (args.length < 2) {
                        MessageUtils.sendMsg(player, "stuck.usage-visita");
                        return true;
                    }
                    handleVisit(player, args[1]);
                    return true;
                }
            }
        }

        startCountdown(player);
        return true;
    }

    private void startCountdown(Player player) {
        if (sessions.containsKey(player.getUniqueId())) {
            sessions.get(player.getUniqueId()).cancel();
            sessions.remove(player.getUniqueId());
        }

        int totalSeconds = plugin.getConfig().getInt("stuck.countdown-seconds", 120);
        Location start = player.getLocation().clone();
        StuckSession session = new StuckSession(player.getUniqueId(), start, totalSeconds);
        sessions.put(player.getUniqueId(), session);
        session.start();
    }

    private void handleAdd(Player player, String name) {
        String key = name.toLowerCase();
        if (safes.containsKey(key)) {
            MessageUtils.sendMsg(player, "stuck.safe-exists", "name", name);
            return;
        }
        safes.put(key, player.getLocation().clone());
        saveSafes();
        MessageUtils.sendMsg(player, "stuck.safe-added", "name", name);
    }

    private void handleRemove(Player player, String name) {
        String key = name.toLowerCase();
        if (!safes.containsKey(key)) {
            MessageUtils.sendMsg(player, "stuck.safe-not-found", "name", name);
            return;
        }
        safes.remove(key);
        saveSafes();
        MessageUtils.sendMsg(player, "stuck.safe-removed", "name", name);
    }

    private void handleList(Player player) {
        if (safes.isEmpty()) {
            MessageUtils.sendMsg(player, "stuck.safe-list-empty");
            return;
        }
        MessageUtils.sendMsg(player, "stuck.safe-list-header",
                "count", String.valueOf(safes.size()));
        for (Map.Entry<String, Location> e : safes.entrySet()) {
            Location l = e.getValue();
            MessageUtils.sendMsg(player, "stuck.safe-list-entry",
                    "name", e.getKey(),
                    "world", l.getWorld().getName(),
                    "x", String.valueOf(l.getBlockX()),
                    "y", String.valueOf(l.getBlockY()),
                    "z", String.valueOf(l.getBlockZ()));
        }
    }

    private void handleVisit(Player player, String name) {
        String key = name.toLowerCase();
        Location loc = safes.get(key);
        if (loc == null) {
            MessageUtils.sendMsg(player, "stuck.safe-not-found", "name", name);
            return;
        }
        player.teleport(loc);
        MessageUtils.sendMsg(player, "stuck.safe-visited", "name", name);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        StuckSession session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null) return;
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) return;

        session.cancelWithMove();
        sessions.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        StuckSession session = sessions.remove(event.getPlayer().getUniqueId());
        if (session != null) session.cancel();
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player p) || !p.hasPermission(ADMIN_PERM)) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (String s : List.of("aggiungisafe", "rimuovisafe", "lista", "visita")) {
                if (s.startsWith(prefix)) out.add(s);
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("rimuovisafe") || args[0].equalsIgnoreCase("visita"))) {
            String prefix = args[1].toLowerCase();
            for (String s : safes.keySet()) {
                if (s.startsWith(prefix)) out.add(s);
            }
        }
        return out;
    }

    private Location findClosestSafe(Player p) {
        if (safes.isEmpty()) return null;
        Location origin = p.getLocation();
        Location closest = null;
        double bestDist = Double.MAX_VALUE;
        for (Location safe : safes.values()) {
            if (!safe.getWorld().equals(origin.getWorld())) continue;
            double d = safe.distanceSquared(origin);
            if (d < bestDist) {
                bestDist = d;
                closest = safe;
            }
        }
        if (closest == null) {
            closest = safes.values().iterator().next();
        }
        return closest;
    }

    private class StuckSession {
        private final UUID uuid;
        private final Location startLocation;
        private int remaining;
        private BukkitTask task;

        StuckSession(UUID uuid, Location startLocation, int seconds) {
            this.uuid = uuid;
            this.startLocation = startLocation;
            this.remaining = seconds;
        }

        void start() {
            task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) { cancel(); return; }

                if (remaining <= 0) {
                    complete(p);
                    cancel();
                    return;
                }

                MessageUtils.title(p,
                        MessageUtils.get("stuck.wait-title"),
                        MessageUtils.get("stuck.wait-subtitle", "seconds", String.valueOf(remaining)),
                        0, 40, 10);
                remaining--;
            }, 0L, 20L);
        }

        void cancelWithMove() {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                MessageUtils.title(p,
                        MessageUtils.get("stuck.cancelled-title"),
                        MessageUtils.get("stuck.cancelled-subtitle"),
                        10, 60, 10);
            }
            cancel();
        }

        void cancel() {
            if (task != null) { task.cancel(); task = null; }
        }

        void complete(Player p) {
            Location safe = findClosestSafe(p);
            if (safe == null) {
                MessageUtils.sendMsg(p, "stuck.no-safe-configured");
                return;
            }
            p.teleport(safe);
            MessageUtils.sendMsg(p, "stuck.completed");
        }
    }
}