package dev.breach.cherryCore.commands.staff;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClearCommand implements CommandExecutor {

    private final CherryCore plugin;

    public ClearCommand(CherryCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("cherrycore.clear")) {
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

        if (!sender.hasPermission("cherrycore.clear.others")) {
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
                "&fInventario di &d" + target.getName() + " &fsvuotato.");
        MessageUtils.sendPrefixed(target,
                "&fInventario svuotato da &d" + sender.getName() + "&f.");
        return true;
    }
}