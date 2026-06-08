package dev.breach.cherryCore.commands.utils;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;

import java.util.Set;

public class WarpCommands {

    private final CherryCore plugin;

    public WarpCommands(CherryCore plugin) {
        this.plugin = plugin;
    }

    public CommandExecutor warp() {
        return (sender, cmd, label, args) -> {
            if (!(sender instanceof Player p)) {
                MessageUtils.send(sender, "&cSolo i giocatori.");
                return true;
            }
            if (args.length == 0) {
                MessageUtils.sendPrefixed(p, "&cUsa: /warp <nome> oppure /warps");
                return true;
            }
            Location loc = plugin.getDataManager().getWarp(args[0]);
            if (loc == null) {
                MessageUtils.sendPrefixed(p, "&cWarp &d" + args[0] + " &cnon trovato.");
                return true;
            }
            p.teleport(loc);
            MessageUtils.sendPrefixed(p, "&fTeleportato a &d" + args[0] + "&f.");
            return true;
        };
    }

    public CommandExecutor setwarp() {
        return (sender, cmd, label, args) -> {
            if (!(sender instanceof Player p)) {
                MessageUtils.send(sender, "&cSolo i giocatori.");
                return true;
            }
            if (args.length == 0) {
                MessageUtils.sendPrefixed(p, "&cUsa: /setwarp <nome>");
                return true;
            }
            plugin.getDataManager().setWarp(args[0], p.getLocation(), p.getName());
            MessageUtils.sendPrefixed(p, "&fWarp &d" + args[0] + " &fcreato.");
            return true;
        };
    }

    public CommandExecutor delwarp() {
        return (sender, cmd, label, args) -> {
            if (args.length == 0) {
                MessageUtils.sendPrefixed(sender, "&cUsa: /delwarp <nome>");
                return true;
            }
            if (plugin.getDataManager().getWarp(args[0]) == null) {
                MessageUtils.sendPrefixed(sender, "&cWarp non trovato.");
                return true;
            }
            plugin.getDataManager().delWarp(args[0]);
            MessageUtils.sendPrefixed(sender, "&fWarp &d" + args[0] + " &feliminato.");
            return true;
        };
    }

    public CommandExecutor warps() {
        return (sender, cmd, label, args) -> {
            Set<String> list = plugin.getDataManager().listWarps();
            if (list.isEmpty()) {
                MessageUtils.sendPrefixed(sender, "&cNessun warp disponibile.");
                return true;
            }
            StringBuilder sb = new StringBuilder();
            for (String n : list) sb.append("&d").append(n).append("&f, ");
            MessageUtils.sendPrefixed(sender, "&fWarp disponibili (" + list.size() + "): " + sb);
            return true;
        };
    }
}