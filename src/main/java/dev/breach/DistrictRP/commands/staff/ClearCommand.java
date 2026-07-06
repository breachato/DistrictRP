package dev.breach.DistrictRP.commands.staff;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClearCommand implements CommandExecutor {

    private final DistrictRP plugin;

    public ClearCommand(DistrictRP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("DistrictRP.clear")) {
            MessageUtils.send(sender, "&cNon hai il permesso.");
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                MessageUtils.send(sender, "&cUsa: /clear <player>");
                return true;
            }
            p.getInventory().clear();
            MessageUtils.sendPrefixed(p, "&fInventario svuotato.");
            return true;
        }

        if (!sender.hasPermission("DistrictRP.clear.others")) {
            MessageUtils.sendPrefixed(sender, "&cNon hai il permesso.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            MessageUtils.send(sender, "&cGiocatore non trovato.");
            return true;
        }

        target.getInventory().clear();
        MessageUtils.sendPrefixed(sender,
                "&fInventario di &e" + target.getName() + " &fsvuotato.");
        MessageUtils.sendPrefixed(target,
                "&fInventario svuotato da &e" + sender.getName() + "&f.");
        return true;
    }
}