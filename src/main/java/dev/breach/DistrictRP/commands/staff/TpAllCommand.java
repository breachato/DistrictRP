package dev.breach.DistrictRP.commands.staff;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TpAllCommand implements CommandExecutor {

    private final DistrictRP plugin;

    public TpAllCommand(DistrictRP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player p)) {
            MessageUtils.send(sender, "&cSolo i giocatori.");
            return true;
        }
        if (!p.hasPermission("DistrictRP.tpall")) {
            MessageUtils.send(p, "&cNon hai il permesso.");
            return true;
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(p)) {
                online.teleport(p.getLocation());
                MessageUtils.sendPrefixed(online,
                        "&fTeleportato da &e" + p.getName() + "&f.");
            }
        }
        MessageUtils.sendPrefixed(p, "&fTutti teleportati da te.");
        return true;
    }
}