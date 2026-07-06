package dev.breach.DistrictRP.commands.roleplay.profile;

import dev.breach.DistrictRP.DistrictRP;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RPProfileManager {

    private final DistrictRP plugin;
    private final File file;
    private FileConfiguration config;
    private final Map<UUID, RPProfile> cache = new HashMap<>();

    public RPProfileManager(DistrictRP plugin) {
        this.plugin = plugin;
        File dir = new File(plugin.getDataFolder(), "roleplay");
        if (!dir.exists()) dir.mkdirs();
        this.file = new File(dir, "profiles.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        loadAll();
    }

    private String getDefaultJob() {
        return plugin.getConfig().getString("profile.default-job", "DISOCCUPATO");
    }

    private void loadAll() {
        ConfigurationSection sec = config.getConfigurationSection("profiles");
        if (sec == null) return;
        String defJob = getDefaultJob();
        for (String key : sec.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                RPProfile p = new RPProfile(uuid, defJob);
                String base = "profiles." + key + ".";
                p.setRpName(config.getString(base + "rpname", null));
                p.setRpSurname(config.getString(base + "rpsurname", null));
                p.setJob(config.getString(base + "job", defJob));
                p.setIcAge(config.getInt(base + "ic-age", 18));
                p.setIcGender(config.getString(base + "ic-gender", "M"));
                p.setIcBirthday(config.getString(base + "ic-birthday", ""));
                p.setIcNationality(config.getString(base + "ic-nationality", "Italiana"));
                p.setIcBio(config.getString(base + "ic-bio", ""));
                p.setMoney(config.getLong(base + "money", 0));
                p.setBank(config.getLong(base + "bank", 0));
                p.setDebt(config.getLong(base + "debt", 0));
                p.setAzienda(config.getString(base + "azienda", null));
                p.setAziendaRuolo(config.getString(base + "azienda-ruolo", null));
                p.setAziendaSalary(config.getLong(base + "azienda-salary", 0));
                p.setPhone(config.getString(base + "phone", ""));
                p.setDiscordId(config.getString(base + "discord-id", ""));
                p.setTelegramId(config.getString(base + "telegram-id", ""));
                p.setLastKnownAddress(config.getString(base + "address", ""));
                p.setVehicle(config.getString(base + "vehicle", ""));
                p.setVehiclePlate(config.getString(base + "vehicle-plate", ""));
                p.setFedina(config.getInt(base + "fedina", 0));
                p.setMulte(config.getInt(base + "multe", 0));
                p.setLastCrimeTimestamp(config.getLong(base + "last-crime", 0));
                for (String lic : config.getStringList(base + "licenses")) p.addLicense(lic);
                for (String per : config.getStringList(base + "permessi")) p.addPermesso(per);
                p.setFirstJoin(config.getLong(base + "first-join", System.currentTimeMillis()));
                p.setLastJoin(config.getLong(base + "last-join", System.currentTimeMillis()));
                p.setLastQuit(config.getLong(base + "last-quit", 0));
                p.setReputation(config.getInt(base + "reputation", 100));
                p.setDeaths(config.getInt(base + "deaths", 0));
                p.setKills(config.getInt(base + "kills", 0));
                cache.put(uuid, p);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public RPProfile get(UUID uuid) {
        return cache.computeIfAbsent(uuid, u -> new RPProfile(u, getDefaultJob()));
    }

    public RPProfile get(OfflinePlayer p) {
        return get(p.getUniqueId());
    }

    public Map<UUID, RPProfile> getAll() {
        return cache;
    }

    public void save(UUID uuid) {
        saveAll();
    }

    public void reset(UUID uuid) {
        cache.put(uuid, new RPProfile(uuid, getDefaultJob()));
        saveAll();
    }

    public void saveAll() {
        FileConfiguration newConfig = new YamlConfiguration();
        ConfigurationSection sec = newConfig.createSection("profiles");
        for (Map.Entry<UUID, RPProfile> e : cache.entrySet()) {
            ConfigurationSection ps = sec.createSection(e.getKey().toString());
            RPProfile p = e.getValue();
            ps.set("rpname", p.getRpName());
            ps.set("rpsurname", p.getRpSurname());
            ps.set("job", p.getJob());
            ps.set("ic-age", p.getIcAge());
            ps.set("ic-gender", p.getIcGender());
            ps.set("ic-birthday", p.getIcBirthday());
            ps.set("ic-nationality", p.getIcNationality());
            ps.set("ic-bio", p.getIcBio());
            ps.set("money", p.getMoney());
            ps.set("bank", p.getBank());
            ps.set("debt", p.getDebt());
            ps.set("azienda", p.getAzienda());
            ps.set("azienda-ruolo", p.getAziendaRuolo());
            ps.set("azienda-salary", p.getAziendaSalary());
            ps.set("phone", p.getPhone());
            ps.set("discord-id", p.getDiscordId());
            ps.set("telegram-id", p.getTelegramId());
            ps.set("address", p.getLastKnownAddress());
            ps.set("vehicle", p.getVehicle());
            ps.set("vehicle-plate", p.getVehiclePlate());
            ps.set("fedina", p.getFedina());
            ps.set("multe", p.getMulte());
            ps.set("last-crime", p.getLastCrimeTimestamp());
            ps.set("licenses", new ArrayList<>(p.getLicenses()));
            ps.set("permessi", new ArrayList<>(p.getPermessi()));
            ps.set("first-join", p.getFirstJoin());
            ps.set("last-join", p.getLastJoin());
            ps.set("last-quit", p.getLastQuit());
            ps.set("reputation", p.getReputation());
            ps.set("deaths", p.getDeaths());
            ps.set("kills", p.getKills());
        }
        this.config = newConfig;
        try { config.save(file); }
        catch (IOException e) { plugin.getLogger().warning("Errore salvataggio profiles.yml: " + e.getMessage()); }
    }
}