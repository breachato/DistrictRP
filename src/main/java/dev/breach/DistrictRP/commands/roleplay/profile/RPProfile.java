package dev.breach.DistrictRP.commands.roleplay.profile;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RPProfile {

    private final UUID uuid;

    private String rpName;
    private String rpSurname;
    private String job;
    private int icAge;
    private String icGender;
    private String icBirthday;
    private String icNationality;
    private String icBio;

    private long money;
    private long bank;
    private long debt;

    private String azienda;
    private String aziendaRuolo;
    private long aziendaSalary;

    private String phone;
    private String discordId;
    private String telegramId;

    private String lastKnownAddress;
    private String vehicle;
    private String vehiclePlate;

    private int fedina;
    private int multe;
    private long lastCrimeTimestamp;

    private final Set<String> licenses = new HashSet<>();
    private final Set<String> permessi = new HashSet<>();

    private long firstJoin;
    private long lastJoin;
    private long lastQuit;

    private int reputation;
    private int deaths;
    private int kills;

    public RPProfile(UUID uuid) {
        this(uuid, "DISOCCUPATO");
    }

    public RPProfile(UUID uuid, String defaultJob) {
        this.uuid = uuid;
        this.rpName = null;
        this.rpSurname = null;
        this.job = defaultJob;
        this.icAge = 18;
        this.icGender = "M";
        this.icBirthday = "";
        this.icNationality = "Italiana";
        this.icBio = "";
        this.money = 0;
        this.bank = 0;
        this.debt = 0;
        this.azienda = null;
        this.aziendaRuolo = null;
        this.aziendaSalary = 0;
        this.phone = "";
        this.discordId = "";
        this.telegramId = "";
        this.lastKnownAddress = "";
        this.vehicle = "";
        this.vehiclePlate = "";
        this.fedina = 0;
        this.multe = 0;
        this.lastCrimeTimestamp = 0;
        this.firstJoin = System.currentTimeMillis();
        this.lastJoin = System.currentTimeMillis();
        this.lastQuit = 0;
        this.reputation = 100;
        this.deaths = 0;
        this.kills = 0;
    }

    public UUID getUuid() { return uuid; }

    public String getRpName() { return rpName; }
    public void setRpName(String rpName) { this.rpName = rpName; }
    public boolean hasRpName() { return rpName != null && !rpName.isEmpty(); }

    public String getRpSurname() { return rpSurname; }
    public void setRpSurname(String s) { this.rpSurname = s; }

    public String getRpFullName() {
        if (rpName == null) return "";
        return rpSurname != null && !rpSurname.isEmpty() ? rpName + " " + rpSurname : rpName;
    }

    public String getJob() { return job; }
    public void setJob(String job) { this.job = job; }

    public int getIcAge() { return icAge; }
    public void setIcAge(int icAge) { this.icAge = icAge; }

    public String getIcGender() { return icGender; }
    public void setIcGender(String g) { this.icGender = g; }

    public String getIcBirthday() { return icBirthday; }
    public void setIcBirthday(String b) { this.icBirthday = b; }

    public String getIcNationality() { return icNationality; }
    public void setIcNationality(String n) { this.icNationality = n; }

    public String getIcBio() { return icBio; }
    public void setIcBio(String b) { this.icBio = b; }

    public long getMoney() { return money; }
    public void setMoney(long money) { this.money = money; }
    public void addMoney(long amount) { this.money += amount; }
    public boolean subtractMoney(long amount) {
        if (this.money < amount) return false;
        this.money -= amount;
        return true;
    }

    public long getBank() { return bank; }
    public void setBank(long bank) { this.bank = bank; }
    public void addBank(long amount) { this.bank += amount; }
    public boolean subtractBank(long amount) {
        if (this.bank < amount) return false;
        this.bank -= amount;
        return true;
    }

    public long getDebt() { return debt; }
    public void setDebt(long debt) { this.debt = debt; }

    public long getTotalWealth() { return money + bank - debt; }

    public String getAzienda() { return azienda; }
    public void setAzienda(String azienda) { this.azienda = azienda; }
    public boolean hasAzienda() { return azienda != null && !azienda.isEmpty(); }

    public String getAziendaRuolo() { return aziendaRuolo; }
    public void setAziendaRuolo(String ruolo) { this.aziendaRuolo = ruolo; }

    public long getAziendaSalary() { return aziendaSalary; }
    public void setAziendaSalary(long s) { this.aziendaSalary = s; }

    public String getPhone() { return phone; }
    public void setPhone(String p) { this.phone = p; }

    public String getDiscordId() { return discordId; }
    public void setDiscordId(String d) { this.discordId = d; }

    public String getTelegramId() { return telegramId; }
    public void setTelegramId(String t) { this.telegramId = t; }

    public String getLastKnownAddress() { return lastKnownAddress; }
    public void setLastKnownAddress(String a) { this.lastKnownAddress = a; }

    public String getVehicle() { return vehicle; }
    public void setVehicle(String v) { this.vehicle = v; }

    public String getVehiclePlate() { return vehiclePlate; }
    public void setVehiclePlate(String p) { this.vehiclePlate = p; }

    public int getFedina() { return fedina; }
    public void setFedina(int f) { this.fedina = f; }
    public void addFedina(int amount) { this.fedina += amount; }

    public int getMulte() { return multe; }
    public void setMulte(int m) { this.multe = m; }
    public void addMulta() { this.multe++; }

    public long getLastCrimeTimestamp() { return lastCrimeTimestamp; }
    public void setLastCrimeTimestamp(long t) { this.lastCrimeTimestamp = t; }

    public Set<String> getLicenses() { return licenses; }
    public boolean hasLicense(String id) { return licenses.contains(id.toLowerCase()); }
    public void addLicense(String id) { licenses.add(id.toLowerCase()); }
    public void removeLicense(String id) { licenses.remove(id.toLowerCase()); }

    public Set<String> getPermessi() { return permessi; }
    public boolean hasPermesso(String id) { return permessi.contains(id.toLowerCase()); }
    public void addPermesso(String id) { permessi.add(id.toLowerCase()); }
    public void removePermesso(String id) { permessi.remove(id.toLowerCase()); }

    public long getFirstJoin() { return firstJoin; }
    public void setFirstJoin(long t) { this.firstJoin = t; }

    public long getLastJoin() { return lastJoin; }
    public void setLastJoin(long t) { this.lastJoin = t; }

    public long getLastQuit() { return lastQuit; }
    public void setLastQuit(long t) { this.lastQuit = t; }

    public int getReputation() { return reputation; }
    public void setReputation(int r) { this.reputation = r; }
    public void addReputation(int amount) { this.reputation += amount; }

    public int getDeaths() { return deaths; }
    public void setDeaths(int d) { this.deaths = d; }
    public void addDeath() { this.deaths++; }

    public int getKills() { return kills; }
    public void setKills(int k) { this.kills = k; }
    public void addKill() { this.kills++; }
}