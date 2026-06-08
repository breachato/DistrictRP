package dev.breach.cherryCore.commands.staff;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class InvseeCommand implements CommandExecutor {

    private final CherryCore plugin;

    public InvseeCommand(CherryCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player p)) {
            MessageUtils.send(sender, "&cSolo i giocatori.");
            return true;
        }
        if (!p.hasPermission("cherrycore.invsee")) {
            MessageUtils.send(p, "&cNon hai il permesso.");
            return true;
        }
        if (args.length == 0) {
            MessageUtils.sendPrefixed(p, "&cUsa: /invsee <player>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            MessageUtils.send(p, "&cGiocatore non trovato.");
            return true;
        }
        if (target.equals(p)) {
            MessageUtils.sendPrefixed(p, "&cNon puoi vedere il tuo stesso inventario.");
            return true;
        }

        p.openInventory(target.getInventory());
        MessageUtils.sendPrefixed(p, "&fInventario di &d" + target.getName() + "&f.");
        return true;
    }
}