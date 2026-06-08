package dev.breach.cherryCore.commands.staff;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NickCommand implements CommandExecutor {

    private final CherryCore plugin;

    public NickCommand(CherryCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("cherrycore.nick")) {
            MessageUtils.send(sender, "&cNon hai il permesso.");
            return true;
        }
        if (args.length == 0) {
            MessageUtils.sendPrefixed(sender, "&cUsa: /nick <nick> [player]");
            return true;
        }

        String nick = ChatColor.translateAlternateColorCodes('&', args[0]);
        Player target;

        if (args.length > 1) {
            if (!sender.hasPermission("cherrycore.nick.others")) {
                MessageUtils.sendPrefixed(sender, "&cNon hai il permesso.");
                return true;
            }
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                MessageUtils.send(sender, "&cGiocatore non trovato.");
                return true;
            }
        } else {
            if (!(sender instanceof Player p)) {
                MessageUtils.send(sender, "&cUsa: /nick <nick> <player>");
                return true;
            }
            target = p;
        }

        target.setDisplayName(nick);
        target.setPlayerListName(nick);
        if (target.equals(sender)) {
            MessageUtils.sendPrefixed(sender, "&fNick impostato: &d" + nick);
        } else {
            MessageUtils.sendPrefixed(sender,
                    "&fNick di &d" + target.getName() + " &fimpostato: &d" + nick);
        }
        return true;
    }
}