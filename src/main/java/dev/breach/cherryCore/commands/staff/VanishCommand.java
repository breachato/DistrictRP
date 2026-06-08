package dev.breach.cherryCore.commands.staff;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class VanishCommand implements CommandExecutor {

    private final CherryCore plugin;

    public VanishCommand(CherryCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("cherrycore.vanish")) {
            MessageUtils.send(sender, "&cNon hai il permesso.");
            return true;
        }

        Player target;
        if (args.length > 0) {
            if (!sender.hasPermission("cherrycore.vanish.others")) {
                MessageUtils.send(sender, "&cNon hai il permesso.");
                return true;
            }
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                MessageUtils.send(sender, "&cGiocatore non trovato.");
                return true;
            }
        } else {
            if (!(sender instanceof Player p)) {
                MessageUtils.send(sender, "&cUsa: /vanish <player>");
                return true;
            }
            target = p;
        }

        plugin.getVanishManager().toggle(target);
        if (!target.equals(sender)) {
            MessageUtils.sendPrefixed(sender, "&fVanish di &d" + target.getName() + " &ftoggleato.");
        }
        return true;
    }
}