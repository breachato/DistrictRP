package dev.breach.DistrictRP.commands.staff;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpeedCommand implements CommandExecutor {

    private final DistrictRP plugin;

    public SpeedCommand(DistrictRP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("DistrictRP.speed")) {
            MessageUtils.send(sender, "&cNon hai il permesso.");
            return true;
        }
        if (args.length == 0) {
            MessageUtils.sendPrefixed(sender, "&cUsa: /speed <0-10> [player]");
            return true;
        }
        int spd;
        try {
            spd = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            MessageUtils.sendPrefixed(sender, "&cVelocità non valida.");
            return true;
        }
        if (spd < 0 || spd > 10) {
            MessageUtils.sendPrefixed(sender, "&cVelocità tra 0 e 10.");
            return true;
        }

        Player target;
        if (args.length > 1) {
            if (!sender.hasPermission("DistrictRP.speed.others")) {
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
                MessageUtils.send(sender, "&cUsa: /speed <0-10> <player>");
                return true;
            }
            target = p;
        }

        float val = spd / 10f;
        target.setWalkSpeed(val);
        target.setFlySpeed(val);
        MessageUtils.sendPrefixed(target, "&fVelocità impostata a &e" + spd + "&f.");
        if (!target.equals(sender)) {
            MessageUtils.sendPrefixed(sender,
                    "&fVelocità di &e" + target.getName() + " &fimpostata a &e" + spd + "&f.");
        }
        return true;
    }
}