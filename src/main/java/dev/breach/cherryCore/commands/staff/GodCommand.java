package dev.breach.cherryCore.commands.staff;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class GodCommand implements CommandExecutor {

    private final CherryCore plugin;

    public GodCommand(CherryCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("cherrycore.god")) {
            MessageUtils.send(sender, "&cNon hai il permesso.");
            return true;
        }

        Player target;
        boolean other = false;
        if (args.length > 0) {
            if (!sender.hasPermission("cherrycore.god.others")) {
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
                MessageUtils.send(sender, "&cUsa: /god <player>");
                return true;
            }
            target = p;
        }

        UUID uuid = target.getUniqueId();
        boolean newState = !Boolean.TRUE.equals(plugin.godMode.get(uuid));
        if (newState) {
            plugin.godMode.put(uuid, true);
            MessageUtils.sendPrefixed(target, "&fGodmode attivato.");
            if (other) MessageUtils.sendPrefixed(sender, "&fGodmode attivato per &d" + target.getName() + "&f.");
        } else {
            plugin.godMode.remove(uuid);
            MessageUtils.sendPrefixed(target, "&cGodmode disattivato.");
            if (other) MessageUtils.sendPrefixed(sender, "&cGodmode disattivato per &d" + target.getName() + "&c.");
        }
        return true;
    }
}