package dev.breach.DistrictRP.functions.profile;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.roleplay.profile.RPProfile;
import dev.breach.DistrictRP.commands.roleplay.profile.RPProfileManager;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProfileScoreboard {

    private final DistrictRP plugin;
    private final Map<UUID, Integer> activeTimers = new HashMap<>();

    public ProfileScoreboard(DistrictRP plugin) {
        this.plugin = plugin;
    }

    public void show(Player viewer, Player target) {
        RPProfileManager pm = plugin.getRoleplay() != null ? plugin.getRoleplay().getProfileManager() : null;
        if (pm == null) return;

        RPProfile profile = pm.get(target.getUniqueId());
        int duration = plugin.getConfig().getInt("profile-scoreboard.duration-seconds", 10);

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        String title = MessageUtils.color(plugin.getConfig().getString(
                        "profile-scoreboard.title", "&#FCD05C&lᴘʀᴏꜰɪʟᴏ"))
                .replace("%player%", target.getName());

        Objective obj = board.registerNewObjective("drp_profile", Criteria.DUMMY, title);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> lines = plugin.getConfig().getStringList("profile-scoreboard.lines");
        if (lines.isEmpty()) {
            lines = List.of(
                    "",
                    "&7ɴᴏᴍᴇ ɪᴄ &8» &f%rpfullname%",
                    "&7ᴄᴏᴅɪᴄᴇ ꜰɪꜱᴄᴀʟᴇ &8» &f%cf%",
                    "&7ᴇᴛà &8» &f%age%",
                    "&7ꜱᴇꜱꜱᴏ &8» &f%gender%",
                    "&7ɴᴀᴢɪᴏɴᴀʟɪᴛà &8» &f%nationality%",
                    "&7ʟᴀᴠᴏʀᴏ &8» &f%job%",
                    "&7ᴀᴢɪᴇɴᴅᴀ &8» &f%azienda%",
                    "&7ʀᴜᴏʟᴏ &8» &f%ruolo%",
                    "&7ꜱᴏʟᴅɪ &8» &f%money%",
                    "",
                    "&#FCD05C&lᴅɪꜱᴛʀɪᴄᴛʀᴘ"
            );
        }

        String rpName = profile.hasRpName() ? profile.getRpName() : target.getName();
        String rpSurname = profile.getRpSurname() == null ? "" : profile.getRpSurname();
        String rpFullName = profile.getRpFullName().isEmpty() ? target.getName() : profile.getRpFullName();
        String defaultJob = plugin.getConfig().getString("profile.default-job", "&7Disoccupato");
        String job = (profile.getJob() == null || profile.getJob().isEmpty()) ? defaultJob : profile.getJob();

        int score = lines.size();
        for (String line : lines) {
            String processed = line
                    .replace("%player%", target.getName())
                    .replace("%rpname%", rpName)
                    .replace("%rpsurname%", rpSurname)
                    .replace("%rpfullname%", rpFullName)
                    .replace("%cf%", target.getName())
                    .replace("%age%", String.valueOf(profile.getIcAge()))
                    .replace("%gender%", profile.getIcGender() == null ? "-" : profile.getIcGender())
                    .replace("%nationality%", profile.getIcNationality() == null ? "-" : profile.getIcNationality())
                    .replace("%birthday%", profile.getIcBirthday() == null ? "-" : profile.getIcBirthday())
                    .replace("%bio%", profile.getIcBio() == null ? "-" : profile.getIcBio())
                    .replace("%job%", job)
                    .replace("%azienda%", profile.getAzienda() == null ? "-" : profile.getAzienda())
                    .replace("%ruolo%", profile.getAziendaRuolo() == null ? "-" : profile.getAziendaRuolo())
                    .replace("%money%", String.valueOf(profile.getMoney()))
                    .replace("%bank%", String.valueOf(profile.getBank()))
                    .replace("%phone%", profile.getPhone() == null ? "-" : profile.getPhone())
                    .replace("%reputation%", String.valueOf(profile.getReputation()))
                    .replace("%kills%", String.valueOf(profile.getKills()))
                    .replace("%deaths%", String.valueOf(profile.getDeaths()));

            processed = MessageUtils.color(processed);

            String unique = processed;
            while (board.getEntries().contains(unique)) {
                unique = unique + " ";
            }

            obj.getScore(unique).setScore(score--);
        }

        Scoreboard previousBoard = viewer.getScoreboard();
        viewer.setScoreboard(board);

        Integer existingTask = activeTimers.remove(viewer.getUniqueId());
        if (existingTask != null) Bukkit.getScheduler().cancelTask(existingTask);

        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (viewer.isOnline()) {
                viewer.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            }
            activeTimers.remove(viewer.getUniqueId());
        }, duration * 20L).getTaskId();

        activeTimers.put(viewer.getUniqueId(), taskId);
    }

    public void hide(Player viewer) {
        Integer taskId = activeTimers.remove(viewer.getUniqueId());
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
        if (viewer.isOnline()) {
            viewer.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
    }
}