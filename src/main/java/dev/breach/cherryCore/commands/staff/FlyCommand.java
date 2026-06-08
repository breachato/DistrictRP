package dev.breach.cherryCore.commands.staff;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FlyCommand implements CommandExecutor {

    private final CherryCore plugin;

    public FlyCommand(CherryCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("cherrycore.fly")) {
            MessageUtils.send(sender, "&cNon hai il permesso.");
            return true;
        }

        Player target;
        boolean other = false;

        if (args.length > 0) {
            if (!sender.hasPermission("cherrycore.fly.others")) {
                MessageUtils.sendPrefixed(sender, "&cNon hai il permesso.");
                return true;
            }
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                MessageUtils.send(sender, "&cGiocatore non trovato.");
                return true;
            }
            other = true;
        } else {
            if (!(sender instanceof Player p)) {
                MessageUtils.send(sender, "&cUsa: /fly <player>");
                return true;
            }
            target = p;
        }

        boolean newState = !target.getAllowFlight();
        target.setAllowFlight(newState);
        target.setFlying(newState);

        if (newState) {
            MessageUtils.sendPrefixed(target, "&fVolo attivato.");
            if (other) MessageUtils.sendPrefixed(sender, "&fVolo attivato per &d" + target.getName() + "&f.");
        } else {
            MessageUtils.sendPrefixed(target, "&cVolo disattivato.");
            if (other) MessageUtils.sendPrefixed(sender, "&cVolo disattivato per &d" + target.getName() + "&c.");
        }
        return true;
    }
}