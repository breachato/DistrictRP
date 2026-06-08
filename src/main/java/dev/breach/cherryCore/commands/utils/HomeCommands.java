package dev.breach.cherryCore.commands.utils;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;

import java.util.Set;

public class HomeCommands {

    private final CherryCore plugin;

    public HomeCommands(CherryCore plugin) {
        this.plugin = plugin;
    }

    public CommandExecutor home() {
        return (sender, cmd, label, args) -> {
            if (!(sender instanceof Player p)) {
                MessageUtils.send(sender, "&cSolo i giocatori.");
                return true;
            }
            String name = args.length > 0 ? args[0] : "home";
            Location loc = plugin.getDataManager().getHome(p.getUniqueId(), name);
            if (loc == null) {
                MessageUtils.sendPrefixed(p, "&cHome &d" + name + " &cnon trovata.");
                return true;
            }
            p.teleport(loc);
            MessageUtils.sendPrefixed(p, "&fTeleportato a &d" + name + "&f.");
            return true;
        };
    }

    public CommandExecutor sethome() {
        return (sender, cmd, label, args) -> {
            if (!(sender instanceof Player p)) {
                MessageUtils.send(sender, "&cSolo i giocatori.");
                return true;
            }
            String name = args.length > 0 ? args[0] : "home";
            plugin.getDataManager().setHome(p.getUniqueId(), name, p.getLocation());
            MessageUtils.sendPrefixed(p, "&fHome &d" + name + " &fimpostata.");
            return true;
        };
    }

    public CommandExecutor delhome() {
        return (sender, cmd, label, args) -> {
            if (!(sender instanceof Player p)) {
                MessageUtils.send(sender, "&cSolo i giocatori.");
                return true;
            }
            String name = args.length > 0 ? args[0] : "home";
            if (plugin.getDataManager().getHome(p.getUniqueId(), name) == null) {
                MessageUtils.sendPrefixed(p, "&cHome non trovata.");
                return true;
            }
            plugin.getDataManager().delHome(p.getUniqueId(), name);
            MessageUtils.sendPrefixed(p, "&fHome &d" + name + " &feliminata.");
            return true;
        };
    }

    public CommandExecutor homes() {
        return (sender, cmd, label, args) -> {
            if (!(sender instanceof Player p)) {
                MessageUtils.send(sender, "&cSolo i giocatori.");
                return true;
            }
            Set<String> list = plugin.getDataManager().listHomes(p.getUniqueId());
            if (list.isEmpty()) {
                MessageUtils.sendPrefixed(p, "&cNessuna home impostata.");
                return true;
            }
            StringBuilder sb = new StringBuilder();
            for (String n : list) sb.append("&d").append(n).append("&f, ");
            MessageUtils.sendPrefixed(p, "&fLe tue home (" + list.size() + "): " + sb);
            return true;
        };
    }
}