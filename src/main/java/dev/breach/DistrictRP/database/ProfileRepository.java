package dev.breach.DistrictRP.database.repository;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.roleplay.profile.RPProfile;
import dev.breach.DistrictRP.database.Repository;
import dev.breach.DistrictRP.database.tables.ProfilesTable;
import dev.breach.DistrictRP.database.tables.ProfilesTable.ProfileRow;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ProfileRepository implements Repository {

    private final DistrictRP plugin;
    private ProfilesTable table;

    public ProfileRepository(DistrictRP plugin) {
        this.plugin = plugin;
        if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isMariaDb()) {
            this.table = plugin.getDatabaseManager().getTable("profiles", ProfilesTable.class);
        }
    }

    @Override
    public boolean isAvailable() {
        return table != null;
    }

    public CompletableFuture<RPProfile> loadProfile(UUID uuid) {
        if (!isAvailable()) return CompletableFuture.completedFuture(null);
        return table.get(uuid).thenApply(row -> row == null ? null : toProfile(row));
    }

    public CompletableFuture<Boolean> saveProfile(RPProfile profile) {
        if (!isAvailable()) return CompletableFuture.completedFuture(false);
        return table.upsert(toRow(profile));
    }

    public CompletableFuture<Boolean> deleteProfile(UUID uuid) {
        if (!isAvailable()) return CompletableFuture.completedFuture(false);
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
}