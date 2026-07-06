package dev.breach.DistrictRP.commands.roleplay.stafflist;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StaffListCommand implements CommandExecutor {

    private final DistrictRP plugin;

    public StaffListCommand(DistrictRP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<String> order = plugin.getConfig().getStringList("stafflist.order");
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("stafflist.ranks");
        if (ranks == null) {
            MessageUtils.sendMsg(sender, "stafflist.empty");
            return true;
        }

        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (String rank : order) grouped.put(rank, new ArrayList<>());

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (plugin.getVanishManager() != null
                    && plugin.getVanishManager().isVanished(online.getUniqueId())) continue;
            String rank = resolveRank(online, order, ranks);
            if (rank != null) {
                grouped.get(rank).add(online.getName());
            }
        }

        boolean any = grouped.values().stream().anyMatch(l -> !l.isEmpty());
        if (!any) {
            MessageUtils.sendMsg(sender, "stafflist.empty");
            return true;
        }

        String title = plugin.getConfig().getString("stafflist.title", "&#FCD05CLISTA &a&lSTAFF ONLINE");
        String format = plugin.getConfig().getString("stafflist.format", " &8[%symbol%&8] &8» &7%players%");

        sender.sendMessage(MessageUtils.color(title));
        sender.sendMessage("");

        for (String rank : order) {
            List<String> players = grouped.get(rank);
            if (players.isEmpty()) continue;
            String symbol = ranks.getString(rank + ".symbol", rank);
            String line = format
                    .replace("%symbol%", symbol)
                    .replace("%rank%", rank)
                    .replace("%players%", String.join(", ", players));
            sender.sendMessage(MessageUtils.color(line));
        }
        return true;
    }

    private String resolveRank(Player player, List<String> order, ConfigurationSection ranks) {
        for (String rank : order) {
            String perm = ranks.getString(rank + ".permission", "");
            if (!perm.isEmpty() && player.hasPermission(perm)) {
                return rank;
            }
        }
        return null;
    }
}