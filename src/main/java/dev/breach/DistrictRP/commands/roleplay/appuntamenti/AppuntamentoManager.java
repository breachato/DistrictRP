package dev.breach.DistrictRP.commands.roleplay.appuntamenti;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.tables.AppuntamentiTable;
import dev.breach.DistrictRP.database.tables.AppuntamentiTable.Row;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class AppuntamentoManager implements CommandExecutor {

    private final DistrictRP plugin;
    private final File file;
    private FileConfiguration config;
    private final Map<Integer, Appuntamento> appuntamenti = new LinkedHashMap<>();
    private int nextId = 1;

    private AppuntamentiTable table;
    private boolean useDb;

    public AppuntamentoManager(DistrictRP plugin) {
        this.plugin = plugin;
        File dir = new File(plugin.getDataFolder(), "roleplay");
        if (!dir.exists()) dir.mkdirs();
        this.file = new File(dir, "appuntamenti.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.config = YamlConfiguration.loadConfiguration(file);

        var dbm = plugin.getDatabaseManager();
        this.table = (dbm != null && dbm.isMariaDb()) ? dbm.getTable("appuntamenti", AppuntamentiTable.class) : null;
        this.useDb = (table != null);

        if (useDb) {
            plugin.getLogger().info("[Appuntamenti] Storage: MariaDB (con cache locale)");
            loadFromDb();
        } else {
            plugin.getLogger().info("[Appuntamenti] Storage: YAML");
            loadYaml();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendMsg(sender, "general.only-player");
            return true;
        }
        AppuntamentoGUI.startFlow(plugin, this, player);
        return true;
    }

    private void loadYaml() {
        appuntamenti.clear();
        nextId = config.getInt("next-id", 1);
        ConfigurationSection sec = config.getConfigurationSection("appuntamenti");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                int id = Integer.parseInt(key);
                String base = "appuntamenti." + key + ".";
                UUID player = UUID.fromString(config.getString(base + "player"));
                String name = config.getString(base + "player-name", "?");
                String reparto = config.getString(base + "reparto", "");
                String giorno = config.getString(base + "giorno", "");
                String orario = config.getString(base + "orario", "");
                long created = config.getLong(base + "created-at", System.currentTimeMillis());
                appuntamenti.put(id, new Appuntamento(id, player, name, reparto, giorno, orario, created));
            } catch (Exception ignored) {}
        }
    }

    private void loadFromDb() {
        fetchAllDb().thenAccept(list -> {
            appuntamenti.clear();
            int maxId = 0;
            for (Appuntamento a : list) {
                appuntamenti.put(a.getId(), a);
                if (a.getId() > maxId) maxId = a.getId();
            }
            nextId = maxId + 1;
            plugin.getLogger().info("[Appuntamenti] Caricati " + appuntamenti.size() + " appuntamenti dal database.");
        }).exceptionally(t -> {
            plugin.getLogger().warning("[Appuntamenti] Errore caricamento DB: " + t.getMessage());
            return null;
        });
    }

    public void saveAll() {
        if (useDb) return;
        saveAllYaml();
    }

    private void saveAllYaml() {
        FileConfiguration newConfig = new YamlConfiguration();
        newConfig.set("next-id", nextId);
        ConfigurationSection sec = newConfig.createSection("appuntamenti");
        for (Appuntamento a : appuntamenti.values()) {
            ConfigurationSection as = sec.createSection(String.valueOf(a.getId()));
            as.set("player", a.getPlayer().toString());
            as.set("player-name", a.getPlayerName());
            as.set("reparto", a.getReparto());
            as.set("giorno", a.getGiorno());
            as.set("orario", a.getOrario());
            as.set("created-at", a.getCreatedAt());
        }
        this.config = newConfig;
        try { config.save(file); }
        catch (IOException e) { plugin.getLogger().warning("Errore salvataggio appuntamenti.yml: " + e.getMessage()); }
    }

    public Appuntamento create(UUID player, String playerName, String reparto, String giorno, String orario) {
        if (useDb) {
            Integer newId = bookDb(player, playerName, reparto, giorno, orario).join();
            if (newId == null || newId < 0) {
                plugin.getLogger().warning("[Appuntamenti] Book fallito (slot occupato o errore DB)");
                return null;
            }
            Appuntamento a = new Appuntamento(newId, player, playerName, reparto, giorno, orario, System.currentTimeMillis());
            appuntamenti.put(newId, a);
            notifyStaff(a);
            sendBotNotify(a);
            return a;
        } else {
            int id = nextId++;
            Appuntamento a = new Appuntamento(id, player, playerName, reparto, giorno, orario, System.currentTimeMillis());
            appuntamenti.put(id, a);
            saveAll();
            notifyStaff(a);
            sendBotNotify(a);
            return a;
        }
    }

    public boolean isSlotTaken(String reparto, String giorno, String orario) {
        for (Appuntamento a : appuntamenti.values()) {
            if (a.getReparto().equalsIgnoreCase(reparto)
                    && a.getGiorno().equals(giorno)
                    && a.getOrario().equals(orario)) return true;
        }
        if (useDb) {
            try {
                return isSlotTakenDb(reparto, giorno, orario).join();
            } catch (Exception ignored) {}
        }
        return false;
    }

    public boolean cancel(int id) {
        Appuntamento a = appuntamenti.remove(id);
        if (a == null) return false;
        if (useDb) cancelDb(id);
        else saveAll();
        return true;
    }

    public Map<String, String> getReparti() {
        Map<String, String> map = new LinkedHashMap<>();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("appuntamenti.reparti");
        if (sec == null) return map;
        for (String key : sec.getKeys(false)) {
            map.put(key, sec.getString(key + ".display", key));
        }
        return map;
    }

    public Collection<Appuntamento> getAll() { return appuntamenti.values(); }

    public List<Appuntamento> getByReparto(String reparto) {
        List<Appuntamento> out = new ArrayList<>();
        for (Appuntamento a : appuntamenti.values()) {
            if (a.getReparto().equalsIgnoreCase(reparto)) out.add(a);
        }
        return out;
    }

    public int getDaysAhead() {
        return plugin.getConfig().getInt("appuntamenti.days-ahead", 3);
    }

    public String getTimeStart() {
        return plugin.getConfig().getString("appuntamenti.time-slots.start", "20:00");
    }

    public String getTimeEnd() {
        return plugin.getConfig().getString("appuntamenti.time-slots.end", "23:50");
    }

    public int getStepMinutes() {
        return plugin.getConfig().getInt("appuntamenti.time-slots.step-minutes", 5);
    }

    public int getSlotsPerPage() {
        return plugin.getConfig().getInt("appuntamenti.time-slots.slots-per-page", 16);
    }

    public List<String> generateTimeSlots() {
        List<String> slots = new ArrayList<>();
        String[] startParts = getTimeStart().split(":");
        String[] endParts = getTimeEnd().split(":");
        int startMin = Integer.parseInt(startParts[0]) * 60 + Integer.parseInt(startParts[1]);
        int endMin = Integer.parseInt(endParts[0]) * 60 + Integer.parseInt(endParts[1]);
        int step = getStepMinutes();
        for (int m = startMin; m <= endMin; m += step) {
            int h = m / 60;
            int min = m % 60;
            slots.add(String.format("%02d:%02d", h, min));
        }
        return slots;
    }

    public List<String> generateDays() {
        List<String> days = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE d MMMM", Locale.ITALIAN);
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < getDaysAhead(); i++) {
            String day = sdf.format(cal.getTime());
            day = Character.toUpperCase(day.charAt(0)) + day.substring(1);
            days.add(day);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return days;
    }

    private void notifyStaff(Appuntamento a) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("appuntamenti.reparti." + a.getReparto());
        String perm = sec != null ? sec.getString("staff-permission", "") : "";
        String staffPerm = plugin.getConfig().getString("ticket.staff-permission", "DistrictRP.ticket.staff");
        String msg = MessageUtils.get("appuntamenti.staff-notify",
                "player", a.getPlayerName(),
                "reparto", a.getReparto(),
                "day", a.getGiorno(),
                "time", a.getOrario());
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(staffPerm) || (!perm.isEmpty() && p.hasPermission(perm))) {
                p.sendMessage(msg);
            }
        }
    }

    private void sendBotNotify(Appuntamento a) {
        if (plugin.getRoleplay() == null || plugin.getRoleplay().getBotManager() == null) return;
        if (plugin.getRoleplay().getBotManager().isDiscordActive()) {
            plugin.getRoleplay().getBotManager().getDiscordBot().sendAppuntamento(a);
        }
        if (plugin.getRoleplay().getBotManager().isTelegramActive()) {
            plugin.getRoleplay().getBotManager().getTelegramBot().sendAppuntamento(a);
        }
    }

    public boolean isUsingDatabase() { return useDb; }

    // --- accesso DB (ex AppuntamentoRepository) ---

    public java.util.concurrent.CompletableFuture<Integer> bookDb(UUID uuid, String name, String reparto, String giorno, String orario) {
        if (table == null) return java.util.concurrent.CompletableFuture.completedFuture(-1);
        return table.book(uuid, name, reparto, giorno, orario);
    }

    public java.util.concurrent.CompletableFuture<Boolean> cancelDb(int id) {
        if (table == null) return java.util.concurrent.CompletableFuture.completedFuture(false);
        return table.cancel(id);
    }

    public java.util.concurrent.CompletableFuture<List<Appuntamento>> fetchAllDb() {
        if (table == null) return java.util.concurrent.CompletableFuture.completedFuture(new ArrayList<>());
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            List<Appuntamento> out = new ArrayList<>();
            try (var c = plugin.getDatabaseManager().getDataStore().getConnection();
                 var ps = c.prepareStatement("SELECT * FROM " + table.getTableName() + " ORDER BY id ASC")) {
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Row r = new Row();
                        r.id = rs.getInt("id");
                        r.playerUuid = UUID.fromString(rs.getString("player_uuid"));
                        r.playerName = rs.getString("player_name");
                        r.reparto = rs.getString("reparto");
                        r.giorno = rs.getString("giorno");
                        r.orario = rs.getString("orario");
                        r.createdAt = rs.getLong("created_at");
                        out.add(new Appuntamento(r.id, r.playerUuid, r.playerName, r.reparto, r.giorno, r.orario, r.createdAt));
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[Appuntamenti] loadAll: " + e.getMessage());
            }
            return out;
        });
    }

    public java.util.concurrent.CompletableFuture<Boolean> isSlotTakenDb(String reparto, String giorno, String orario) {
        if (table == null) return java.util.concurrent.CompletableFuture.completedFuture(false);
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM " + table.getTableName() +
                    " WHERE reparto=? AND giorno=? AND orario=?";
            try (var c = plugin.getDatabaseManager().getDataStore().getConnection();
                 var ps = c.prepareStatement(sql)) {
                ps.setString(1, reparto);
                ps.setString(2, giorno);
                ps.setString(3, orario);
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1) > 0;
                }
            } catch (Exception ignored) {}
            return false;
        });
    }
}