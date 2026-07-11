package dev.breach.DistrictRP.commands.roleplay.playtime;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.repository.PlaytimeRepository;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlaytimeTracker implements Listener {

    private final DistrictRP plugin;
    private final File file;
    private FileConfiguration config;
    private final Map<UUID, PlaytimeData> cache = new HashMap<>();
    private BukkitTask tickTask;
    private BukkitTask saveTask;

    private PlaytimeRepository repo;
    private boolean useDb;

    public PlaytimeTracker(DistrictRP plugin) {
        this.plugin = plugin;
        File dir = new File(plugin.getDataFolder(), "roleplay");
        if (!dir.exists()) dir.mkdirs();
        this.file = new File(dir, "playtime.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.config = YamlConfiguration.loadConfiguration(file);

        this.repo = new PlaytimeRepository(plugin);
        this.useDb = repo.isAvailable();

        if (useDb) {
            plugin.getLogger().info("[Playtime] Storage: MariaDB (con cache locale)");
        } else {
            plugin.getLogger().info("[Playtime] Storage: YAML");
            loadAllYaml();
        }
    }

    private void loadAllYaml() {
        ConfigurationSection sec = config.getConfigurationSection("players");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                PlaytimeData d = new PlaytimeData();
                String base = "players." + key + ".";
                d.setTotalSeconds(config.getLong(base + "total", 0));
                d.setDailySeconds(config.getLong(base + "daily", 0));
                d.setWeeklySeconds(config.getLong(base + "weekly", 0));
                d.setMonthlySeconds(config.getLong(base + "monthly", 0));
                d.setDailyReset(config.getLong(base + "daily-reset", System.currentTimeMillis()));
                d.setWeeklyReset(config.getLong(base + "weekly-reset", System.currentTimeMillis()));
                d.setMonthlyReset(config.getLong(base + "monthly-reset", System.currentTimeMillis()));
                cache.put(uuid, d);
            } catch (Exception ignored) {}
        }
    }

    public void saveAll() {
        if (useDb) {
            for (Map.Entry<UUID, PlaytimeData> e : cache.entrySet()) {
                repo.save(e.getKey(), e.getValue());
            }
        } else {
            saveAllYaml();
        }
    }

    private void saveAllYaml() {
        FileConfiguration newConfig = new YamlConfiguration();
        ConfigurationSection sec = newConfig.createSection("players");
        for (Map.Entry<UUID, PlaytimeData> e : cache.entrySet()) {
            ConfigurationSection ps = sec.createSection(e.getKey().toString());
            PlaytimeData d = e.getValue();
            ps.set("total", d.getTotalSeconds());
            ps.set("daily", d.getDailySeconds());
            ps.set("weekly", d.getWeeklySeconds());
            ps.set("monthly", d.getMonthlySeconds());
            ps.set("daily-reset", d.getDailyReset());
            ps.set("weekly-reset", d.getWeeklyReset());
            ps.set("monthly-reset", d.getMonthlyReset());
        }
        this.config = newConfig;
        try { config.save(file); }
        catch (IOException ex) { plugin.getLogger().warning("Errore salvataggio playtime.yml: " + ex.getMessage()); }
    }

    public void start() {
        boolean countHere = plugin.getConfig().getBoolean("playtime.count-here", true);
        if (!countHere) {
            plugin.getLogger().info("[Playtime] count-here=false, tracker disabilitato.");
            return;
        }

        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                get(p.getUniqueId()).addSecond();
            }
        }, 20L, 20L);

        int interval = plugin.getConfig().getInt("playtime.save-interval-seconds", 60);
        saveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                this::saveAll, 20L * interval, 20L * interval);
    }

    public void stop() {
        if (tickTask != null) tickTask.cancel();
        if (saveTask != null) saveTask.cancel();
        saveAll();
    }

    public PlaytimeData get(UUID uuid) {
        PlaytimeData cached = cache.get(uuid);
        if (cached != null) return cached;

        if (useDb) {
            PlaytimeData fromDb = repo.load(uuid).join();
            if (fromDb != null) {
                cache.put(uuid, fromDb);
                return fromDb;
            }
        }

        PlaytimeData fresh = new PlaytimeData();
        long now = System.currentTimeMillis();
        fresh.setDailyReset(now);
        fresh.setWeeklyReset(now);
        fresh.setMonthlyReset(now);
        cache.put(uuid, fresh);
        return fresh;
    }

    public void reset(UUID uuid) {
        PlaytimeData d = new PlaytimeData();
        long now = System.currentTimeMillis();
        d.setDailyReset(now);
        d.setWeeklyReset(now);
        d.setMonthlyReset(now);
        cache.put(uuid, d);
        if (useDb) repo.save(uuid, d);
        else saveAllYaml();
    }

    public String format(long seconds) {
        if (seconds <= 0) return "0 secondi";
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(days == 1 ? " giorno, " : " giorni, ");
        if (hours > 0) sb.append(hours).append(hours == 1 ? " ora, " : " ore, ");
        if (minutes > 0) sb.append(minutes).append(minutes == 1 ? " minuto, " : " minuti, ");
        sb.append(secs).append(secs == 1 ? " secondo" : " secondi");
        return sb.toString();
    }

    public String formatTotal(UUID uuid) { return format(get(uuid).getTotalSeconds()); }
    public String formatDaily(UUID uuid) { return format(get(uuid).getDailySeconds()); }
    public String formatWeekly(UUID uuid) { return format(get(uuid).getWeeklySeconds()); }
    public String formatMonthly(UUID uuid) { return format(get(uuid).getMonthlySeconds()); }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        get(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveAll);
    }

    public boolean isUsingDatabase() { return useDb; }
    public PlaytimeRepository getRepository() { return repo; }
}