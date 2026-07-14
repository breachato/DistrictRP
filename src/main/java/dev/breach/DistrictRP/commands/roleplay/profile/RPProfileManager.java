package dev.breach.DistrictRP.commands.roleplay.profile;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.tables.ProfilesTable;
import dev.breach.DistrictRP.database.tables.ProfilesTable.ProfileRow;
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
import java.util.concurrent.CompletableFuture;

public class RPProfileManager {

    private final DistrictRP plugin;
    private final File file;
    private FileConfiguration config;
    private final Map<UUID, RPProfile> cache = new HashMap<>();

    private ProfilesTable table;
    private boolean useDb;

    public RPProfileManager(DistrictRP plugin) {
        this.plugin = plugin;

        File dir = new File(plugin.getDataFolder(), "roleplay");
        if (!dir.exists()) dir.mkdirs();
        this.file = new File(dir, "profiles.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.config = YamlConfiguration.loadConfiguration(file);

        var dbm = plugin.getDatabaseManager();
        this.table = (dbm != null && dbm.isMariaDb()) ? dbm.getTable("profiles", ProfilesTable.class) : null;
        this.useDb = (table != null);

        if (useDb) {
            plugin.getLogger().info("[Profiles] Storage: MariaDB (con cache locale)");
        } else {
            plugin.getLogger().info("[Profiles] Storage: YAML");
            loadAllYaml();
        }
    }

    private String getDefaultJob() {
        return plugin.getConfig().getString("profile.default-job", "DISOCCUPATO");
    }

    private void loadAllYaml() {
        ConfigurationSection sec = config.getConfigurationSection("profiles");
        if (sec == null) return;
        String defJob = getDefaultJob();
        for (String key : sec.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                RPProfile p = readFromYaml(uuid, key, defJob);
                cache.put(uuid, p);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private RPProfile readFromYaml(UUID uuid, String key, String defJob) {
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
        return p;
    }

    public RPProfile get(UUID uuid) {
        RPProfile cached = cache.get(uuid);
        if (cached != null) return cached;

        if (useDb) {
            RPProfile fromDb = loadProfile(uuid).join();
            if (fromDb != null) {
                cache.put(uuid, fromDb);
                return fromDb;
            }
        }

        RPProfile fresh = new RPProfile(uuid, getDefaultJob());
        cache.put(uuid, fresh);
        return fresh;
    }

    public RPProfile get(OfflinePlayer p) { return get(p.getUniqueId()); }
    public Map<UUID, RPProfile> getAll() { return cache; }

    public void save(UUID uuid) {
        RPProfile p = cache.get(uuid);
        if (p == null) return;
        if (useDb) {
            saveProfile(p);
        } else {
            saveAllYaml();
        }
    }

    public void reset(UUID uuid) {
        RPProfile fresh = new RPProfile(uuid, getDefaultJob());
        cache.put(uuid, fresh);
        if (useDb) saveProfile(fresh);
        else saveAllYaml();
    }

    public void saveAll() {
        if (useDb) {
            for (RPProfile p : cache.values()) saveProfile(p);
        } else {
            saveAllYaml();
        }
    }

    private void saveAllYaml() {
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
        catch (IOException e) { plugin.getLogger().warning("Errore saveAll: " + e.getMessage()); }
    }

    public boolean isUsingDatabase() { return useDb; }

    public CompletableFuture<RPProfile> loadProfile(UUID uuid) {
        if (table == null) return CompletableFuture.completedFuture(null);
        return table.get(uuid).thenApply(row -> row == null ? null : toProfile(row));
    }

    public CompletableFuture<Boolean> saveProfile(RPProfile profile) {
        if (table == null) return CompletableFuture.completedFuture(false);
        return table.upsert(toRow(profile));
    }

    public CompletableFuture<Boolean> deleteProfile(UUID uuid) {
        if (table == null) return CompletableFuture.completedFuture(false);
        return table.delete(uuid);
    }

    private RPProfile toProfile(ProfileRow r) {
        RPProfile p = new RPProfile(r.uuid, r.job != null ? r.job : "DISOCCUPATO");
        p.setRpName(r.rpName);
        p.setRpSurname(r.rpSurname);
        p.setJob(r.job);
        p.setIcAge(r.icAge);
        p.setIcGender(r.icGender);
        p.setIcBirthday(r.icBirthday);
        p.setIcNationality(r.icNationality);
        p.setIcBio(r.icBio);
        p.setMoney(r.money);
        p.setBank(r.bank);
        p.setDebt(r.debt);
        p.setAzienda(r.azienda);
        p.setAziendaRuolo(r.aziendaRuolo);
        p.setAziendaSalary(r.aziendaSalary);
        p.setPhone(r.phone);
        p.setDiscordId(r.discordId);
        p.setTelegramId(r.telegramId);
        p.setLastKnownAddress(r.address);
        p.setVehicle(r.vehicle);
        p.setVehiclePlate(r.vehiclePlate);
        p.setFedina(r.fedina);
        p.setMulte(r.multe);
        p.setLastCrimeTimestamp(r.lastCrime);
        if (r.licensesCsv != null && !r.licensesCsv.isEmpty()) {
            for (String lic : r.licensesCsv.split(",")) if (!lic.isEmpty()) p.addLicense(lic);
        }
        if (r.permessiCsv != null && !r.permessiCsv.isEmpty()) {
            for (String per : r.permessiCsv.split(",")) if (!per.isEmpty()) p.addPermesso(per);
        }
        p.setFirstJoin(r.firstJoin);
        p.setLastJoin(r.lastJoin);
        p.setLastQuit(r.lastQuit);
        p.setReputation(r.reputation);
        p.setDeaths(r.deaths);
        p.setKills(r.kills);
        return p;
    }

    private ProfileRow toRow(RPProfile p) {
        ProfileRow r = new ProfileRow();
        r.uuid = p.getUuid();
        r.rpName = p.getRpName();
        r.rpSurname = p.getRpSurname();
        r.job = p.getJob();
        r.icAge = p.getIcAge();
        r.icGender = p.getIcGender();
        r.icBirthday = p.getIcBirthday();
        r.icNationality = p.getIcNationality();
        r.icBio = p.getIcBio();
        r.money = p.getMoney();
        r.bank = p.getBank();
        r.debt = p.getDebt();
        r.azienda = p.getAzienda();
        r.aziendaRuolo = p.getAziendaRuolo();
        r.aziendaSalary = p.getAziendaSalary();
        r.phone = p.getPhone();
        r.discordId = p.getDiscordId();
        r.telegramId = p.getTelegramId();
        r.address = p.getLastKnownAddress();
        r.vehicle = p.getVehicle();
        r.vehiclePlate = p.getVehiclePlate();
        r.fedina = p.getFedina();
        r.multe = p.getMulte();
        r.lastCrime = p.getLastCrimeTimestamp();
        r.licensesCsv = String.join(",", p.getLicenses());
        r.permessiCsv = String.join(",", p.getPermessi());
        r.firstJoin = p.getFirstJoin();
        r.lastJoin = p.getLastJoin();
        r.lastQuit = p.getLastQuit();
        r.reputation = p.getReputation();
        r.deaths = p.getDeaths();
        r.kills = p.getKills();
        return r;
    }

    public FileConfiguration getYamlConfig() { return config; }
    public File getYamlFile() { return file; }
}