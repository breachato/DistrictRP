package dev.breach.DistrictRP.commands.staff;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FlyCommand implements CommandExecutor {

    private final DistrictRP plugin;

    public FlyCommand(DistrictRP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("DistrictRP.fly")) {
            MessageUtils.send(sender, "&cNon hai il permesso.");
            return true;
        }

        Player target;
        boolean other = false;

        if (args.length > 0) {
            if (!sender.hasPermission("DistrictRP.fly.others")) {
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
            if (other) MessageUtils.sendPrefixed(sender, "&fVolo attivato per &e" + target.getName() + "&f.");
        } else {
            MessageUtils.sendPrefixed(target, "&cVolo disattivato.");
            if (other) MessageUtils.sendPrefixed(sender, "&cVolo disattivato per &e" + target.getName() + "&c.");
        }
        return true;
    }
}