package dev.breach.DistrictRP.commands.roleplay.playtime;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.roleplay.profile.RPProfile;
import dev.breach.DistrictRP.commands.roleplay.profile.RPProfileManager;
import dev.breach.DistrictRP.database.tables.PlaytimeTable;
import dev.breach.DistrictRP.database.tables.PlaytimeTable.PlaytimeRow;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlaytimeTracker implements Listener, CommandExecutor {

    private final DistrictRP plugin;
    private final File file;
    private FileConfiguration config;
    private final Map<UUID, PlaytimeData> cache = new HashMap<>();
    private BukkitTask tickTask;
    private BukkitTask saveTask;

    private PlaytimeTable table;
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

        var dbm = plugin.getDatabaseManager();
        this.table = (dbm != null && dbm.isMariaDb()) ? dbm.getTable("playtime", PlaytimeTable.class) : null;
        this.useDb = (table != null);

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
                save(e.getKey(), e.getValue());
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
            PlaytimeData fromDb = load(uuid).join();
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
        if (useDb) save(uuid, d);
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

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        RPProfileManager profileManager = plugin.getRoleplay().getProfileManager();
        OfflinePlayer target;
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                MessageUtils.sendMsg(sender, "general.only-player");
                return true;
            }
            target = p;
        } else {
            target = Bukkit.getOfflinePlayer(args[0]);
            if (target.getName() == null) {
                MessageUtils.sendMsg(sender, "general.player-not-found");
                return true;
            }
        }

        RPProfile profile = profileManager.get(target.getUniqueId());
        String displayName = profile.hasRpName() ? profile.getRpName() : target.getName();
        String status = target.isOnline()
                ? MessageUtils.get("playtime.online")
                : MessageUtils.get("playtime.offline");

        sender.sendMessage(MessageUtils.get("playtime.header", "player", displayName));
        for (String line : MessageUtils.getList("playtime.lines",
                "total", formatTotal(target.getUniqueId()),
                "daily", formatDaily(target.getUniqueId()),
                "weekly", formatWeekly(target.getUniqueId()),
                "monthly", formatMonthly(target.getUniqueId()),
                "status", status)) {
            sender.sendMessage(line);
        }
        return true;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        get(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveAll);
    }

    public boolean isUsingDatabase() { return useDb; }

    public CompletableFuture<PlaytimeData> load(UUID uuid) {
        if (table == null) return CompletableFuture.completedFuture(null);
        return table.get(uuid).thenApply(row -> row == null ? null : toData(row));
    }

    public CompletableFuture<Boolean> save(UUID uuid, PlaytimeData data) {
        if (table == null) return CompletableFuture.completedFuture(false);
        return table.upsert(toRow(uuid, data));
    }

    private PlaytimeData toData(PlaytimeRow r) {
        PlaytimeData d = new PlaytimeData();
        d.setTotalSeconds(r.total);
        d.setDailySeconds(r.daily);
        d.setWeeklySeconds(r.weekly);
        d.setMonthlySeconds(r.monthly);
        d.setDailyReset(r.lastResetDaily);
        d.setWeeklyReset(r.lastResetWeekly);
        d.setMonthlyReset(r.lastResetMonthly);
        return d;
    }

    private PlaytimeRow toRow(UUID uuid, PlaytimeData d) {
        PlaytimeRow r = new PlaytimeRow();
        r.uuid = uuid;
        r.total = d.getTotalSeconds();
        r.daily = d.getDailySeconds();
        r.weekly = d.getWeeklySeconds();
        r.monthly = d.getMonthlySeconds();
        r.lastResetDaily = d.getDailyReset();
        r.lastResetWeekly = d.getWeeklyReset();
        r.lastResetMonthly = d.getMonthlyReset();
        return r;
    }
}