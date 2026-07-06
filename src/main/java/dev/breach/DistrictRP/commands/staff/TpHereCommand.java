package dev.breach.DistrictRP.commands.staff;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TpHereCommand implements CommandExecutor {

    private final DistrictRP plugin;

    public TpHereCommand(DistrictRP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player p)) {
            MessageUtils.send(sender, "&cSolo i giocatori.");
            return true;
        }
        if (!p.hasPermission("DistrictRP.tphere")) {
            MessageUtils.send(p, "&cNon hai il permesso.");
            return true;
        }
        if (args.length == 0) {
            MessageUtils.sendPrefixed(p, "&cUsa: /tphere <player>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            MessageUtils.send(p, "&cGiocatore non trovato.");
            return true;
        }
        if (target.equals(p)) {
            MessageUtils.sendPrefixed(p, "&cNon puoi teleportare te stesso.");
            return true;
        }

        target.teleport(p.getLocation());
        MessageUtils.sendPrefixed(p, "&e" + target.getName() + " &fteleportato da te.");
        MessageUtils.sendPrefixed(target, "&fTeleportato da &e" + p.getName() + "&f.");
        return true;
    }
}