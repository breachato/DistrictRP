package dev.breach.DistrictRP.database.tables;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.DataStore;
import dev.breach.DistrictRP.database.DatabaseTable;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ProfilesTable extends DatabaseTable {

    public ProfilesTable(DistrictRP plugin, DataStore store) {
        super(plugin, store, "profiles");
    }

    @Override
    protected String getCreateTableQuery() {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "rp_name VARCHAR(64) DEFAULT NULL," +
                "rp_surname VARCHAR(128) DEFAULT NULL," +
                "job VARCHAR(64) DEFAULT 'DISOCCUPATO'," +
                "ic_age INT DEFAULT 18," +
                "ic_gender VARCHAR(16) DEFAULT 'M'," +
                "ic_birthday VARCHAR(16) DEFAULT ''," +
                "ic_nationality VARCHAR(64) DEFAULT 'Italiana'," +
                "ic_bio TEXT DEFAULT NULL," +
                "money BIGINT DEFAULT 0," +
                "bank BIGINT DEFAULT 0," +
                "debt BIGINT DEFAULT 0," +
                "azienda VARCHAR(64) DEFAULT NULL," +
                "azienda_ruolo VARCHAR(64) DEFAULT NULL," +
                "azienda_salary BIGINT DEFAULT 0," +
                "phone VARCHAR(32) DEFAULT ''," +
                "discord_id VARCHAR(32) DEFAULT ''," +
                "telegram_id VARCHAR(32) DEFAULT ''," +
                "address VARCHAR(255) DEFAULT ''," +
                "vehicle VARCHAR(64) DEFAULT ''," +
                "vehicle_plate VARCHAR(16) DEFAULT ''," +
                "fedina INT DEFAULT 0," +
                "multe INT DEFAULT 0," +
                "last_crime BIGINT DEFAULT 0," +
                "licenses TEXT DEFAULT NULL," +
                "permessi TEXT DEFAULT NULL," +
                "first_join BIGINT NOT NULL," +
                "last_join BIGINT NOT NULL," +
                "last_quit BIGINT DEFAULT 0," +
                "reputation INT DEFAULT 100," +
                "deaths INT DEFAULT 0," +
                "kills INT DEFAULT 0," +
                "updated_at BIGINT NOT NULL," +
                "INDEX idx_rpname (rp_name)," +
                "INDEX idx_azienda (azienda)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
    }

    public CompletableFuture<Boolean> upsert(ProfileRow r) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO " + tableName + " (uuid, rp_name, rp_surname, job, ic_age, ic_gender, ic_birthday, " +
                    "ic_nationality, ic_bio, money, bank, debt, azienda, azienda_ruolo, azienda_salary, phone, discord_id, " +
                    "telegram_id, address, vehicle, vehicle_plate, fedina, multe, last_crime, licenses, permessi, first_join, " +
                    "last_join, last_quit, reputation, deaths, kills, updated_at) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "rp_name=VALUES(rp_name), rp_surname=VALUES(rp_surname), job=VALUES(job), ic_age=VALUES(ic_age), " +
                    "ic_gender=VALUES(ic_gender), ic_birthday=VALUES(ic_birthday), ic_nationality=VALUES(ic_nationality), " +
                    "ic_bio=VALUES(ic_bio), money=VALUES(money), bank=VALUES(bank), debt=VALUES(debt), azienda=VALUES(azienda), " +
                    "azienda_ruolo=VALUES(azienda_ruolo), azienda_salary=VALUES(azienda_salary), phone=VALUES(phone), " +
                    "discord_id=VALUES(discord_id), telegram_id=VALUES(telegram_id), address=VALUES(address), " +
                    "vehicle=VALUES(vehicle), vehicle_plate=VALUES(vehicle_plate), fedina=VALUES(fedina), multe=VALUES(multe), " +
                    "last_crime=VALUES(last_crime), licenses=VALUES(licenses), permessi=VALUES(permessi), " +
                    "last_join=VALUES(last_join), last_quit=VALUES(last_quit), reputation=VALUES(reputation), " +
                    "deaths=VALUES(deaths), kills=VALUES(kills), updated_at=VALUES(updated_at)";
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                int i = 1;
                ps.setString(i++, r.uuid.toString());
                ps.setString(i++, r.rpName);
                ps.setString(i++, r.rpSurname);
                ps.setString(i++, r.job);
                ps.setInt(i++, r.icAge);
                ps.setString(i++, r.icGender);
                ps.setString(i++, r.icBirthday);
                ps.setString(i++, r.icNationality);
                ps.setString(i++, r.icBio);
                ps.setLong(i++, r.money);
                ps.setLong(i++, r.bank);
                ps.setLong(i++, r.debt);
                ps.setString(i++, r.azienda);
                ps.setString(i++, r.aziendaRuolo);
                ps.setLong(i++, r.aziendaSalary);
                ps.setString(i++, r.phone);
                ps.setString(i++, r.discordId);
                ps.setString(i++, r.telegramId);
                ps.setString(i++, r.address);
                ps.setString(i++, r.vehicle);
                ps.setString(i++, r.vehiclePlate);
                ps.setInt(i++, r.fedina);
                ps.setInt(i++, r.multe);
                ps.setLong(i++, r.lastCrime);
                ps.setString(i++, r.licensesCsv);
                ps.setString(i++, r.permessiCsv);
                ps.setLong(i++, r.firstJoin);
                ps.setLong(i++, r.lastJoin);
                ps.setLong(i++, r.lastQuit);
                ps.setInt(i++, r.reputation);
                ps.setInt(i++, r.deaths);
                ps.setInt(i++, r.kills);
                ps.setLong(i, System.currentTimeMillis());
                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB profiles.upsert] " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<ProfileRow> get(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM " + tableName + " WHERE uuid=? LIMIT 1";
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    ProfileRow r = new ProfileRow();
                    r.uuid = UUID.fromString(rs.getString("uuid"));
                    r.rpName = rs.getString("rp_name");
                    r.rpSurname = rs.getString("rp_surname");
                    r.job = rs.getString("job");
                    r.icAge = rs.getInt("ic_age");
                    r.icGender = rs.getString("ic_gender");
                    r.icBirthday = rs.getString("ic_birthday");
                    r.icNationality = rs.getString("ic_nationality");
                    r.icBio = rs.getString("ic_bio");
                    r.money = rs.getLong("money");
                    r.bank = rs.getLong("bank");
                    r.debt = rs.getLong("debt");
                    r.azienda = rs.getString("azienda");
                    r.aziendaRuolo = rs.getString("azienda_ruolo");
                    r.aziendaSalary = rs.getLong("azienda_salary");
                    r.phone = rs.getString("phone");
                    r.discordId = rs.getString("discord_id");
                    r.telegramId = rs.getString("telegram_id");
                    r.address = rs.getString("address");
                    r.vehicle = rs.getString("vehicle");
                    r.vehiclePlate = rs.getString("vehicle_plate");
                    r.fedina = rs.getInt("fedina");
                    r.multe = rs.getInt("multe");
                    r.lastCrime = rs.getLong("last_crime");
                    r.licensesCsv = rs.getString("licenses");
                    r.permessiCsv = rs.getString("permessi");
                    r.firstJoin = rs.getLong("first_join");
                    r.lastJoin = rs.getLong("last_join");
                    r.lastQuit = rs.getLong("last_quit");
                    r.reputation = rs.getInt("reputation");
                    r.deaths = rs.getInt("deaths");
                    r.kills = rs.getInt("kills");
                    return r;
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB profiles.get] " + e.getMessage());
                return null;
            }
        });
    }

    public CompletableFuture<Boolean> delete(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = getConnection();
                 PreparedStatement ps = c.prepareStatement("DELETE FROM " + tableName + " WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) { return false; }
        });
    }

    public static class ProfileRow {
        public UUID uuid;
        public String rpName, rpSurname, job = "&7Disoccupato";
        public int icAge = 18;
        public String icGender = "M", icBirthday = "", icNationality = "Italiana", icBio = "";
        public long money, bank, debt;
        public String azienda, aziendaRuolo;
        public long aziendaSalary;
        public String phone = "", discordId = "", telegramId = "", address = "", vehicle = "", vehiclePlate = "";
        public int fedina, multe;
        public long lastCrime;
        public String licensesCsv, permessiCsv;
        public long firstJoin, lastJoin, lastQuit;
        public int reputation = 100, deaths, kills;
    }
}