package dev.breach.DistrictRP.commands.roleplay.appuntamenti;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class AppuntamentoManager {

    private final DistrictRP plugin;
    private final File file;
    private FileConfiguration config;
    private final Map<Integer, Appuntamento> appuntamenti = new LinkedHashMap<>();
    private int nextId = 1;

    public AppuntamentoManager(DistrictRP plugin) {
        this.plugin = plugin;
        File dir = new File(plugin.getDataFolder(), "roleplay");
        if (!dir.exists()) dir.mkdirs();
        this.file = new File(dir, "appuntamenti.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private void load() {
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

    public void saveAll() {
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
        int id = nextId++;
        Appuntamento a = new Appuntamento(id, player, playerName, reparto, giorno, orario, System.currentTimeMillis());
        appuntamenti.put(id, a);
        saveAll();
        notifyStaff(a);
        sendBotNotify(a);
        return a;
    }

    public boolean isSlotTaken(String reparto, String giorno, String orario) {
        for (Appuntamento a : appuntamenti.values()) {
            if (a.getReparto().equalsIgnoreCase(reparto)
                    && a.getGiorno().equals(giorno)
                    && a.getOrario().equals(orario)) return true;
        }
        return false;
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
}