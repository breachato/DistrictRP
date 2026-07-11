package dev.breach.DistrictRP.commands.roleplay.playtime;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.roleplay.profile.RPProfile;
import dev.breach.DistrictRP.commands.roleplay.profile.RPProfileManager;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaytimeCommand implements CommandExecutor {

    private final PlaytimeTracker tracker;
    private final RPProfileManager profileManager;

    public PlaytimeCommand(DistrictRP plugin, PlaytimeTracker tracker, RPProfileManager profileManager) {
        this.tracker = tracker;
        this.profileManager = profileManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
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
                "total", tracker.formatTotal(target.getUniqueId()),
                "daily", tracker.formatDaily(target.getUniqueId()),
                "weekly", tracker.formatWeekly(target.getUniqueId()),
                "monthly", tracker.formatMonthly(target.getUniqueId()),
                "status", status)) {
            sender.sendMessage(line);
        }
        return true;
    }
}