package dev.breach.DistrictRP.commands.roleplay.profile;

import dev.breach.DistrictRP.DistrictRP;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

/**
 * API pubblica per la gestione del job dei player.
 * Usa questa classe per leggere/scrivere il job da altri moduli
 * (aziende, FDO, criminalita, ecc.) senza toccare direttamente RPProfile.
 *
 * Esempio d'uso:
 *   JobService jobs = plugin.getRoleplay().getJobService();
 *   jobs.setJob(playerUUID, "POLIZIOTTO");
 *   String job = jobs.getJob(playerUUID);
 */
public class JobService {

    private final DistrictRP plugin;
    private final RPProfileManager profiles;

    public JobService(DistrictRP plugin, RPProfileManager profiles) {
        this.plugin = plugin;
        this.profiles = profiles;
    }

    public String getDefaultJob() {
        return plugin.getConfig().getString("profile.default-job", "DISOCCUPATO");
    }

    public String getJob(UUID uuid) {
        RPProfile p = profiles.get(uuid);
        String j = p.getJob();
        return (j == null || j.isEmpty()) ? getDefaultJob() : j;
    }

    public String getJob(OfflinePlayer player) {
        return getJob(player.getUniqueId());
    }

    public void setJob(UUID uuid, String job) {
        RPProfile p = profiles.get(uuid);
        p.setJob(job == null || job.isEmpty() ? getDefaultJob() : job.toUpperCase());
        profiles.save(uuid);
    }

    public void setJob(OfflinePlayer player, String job) {
        setJob(player.getUniqueId(), job);
    }

    public void clearJob(UUID uuid) {
        setJob(uuid, getDefaultJob());
    }

    public void clearJob(OfflinePlayer player) {
        clearJob(player.getUniqueId());
    }

    public boolean hasJob(UUID uuid) {
        String j = getJob(uuid);
        return !getDefaultJob().equalsIgnoreCase(j);
    }

    public boolean hasJob(OfflinePlayer player) {
        return hasJob(player.getUniqueId());
    }

    public String getJobDisplay(UUID uuid) {
        return capitalize(getJob(uuid));
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        String[] parts = s.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}