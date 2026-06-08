package dev.breach.cherryCore.commands.utils;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;

public class SpawnCommands {

    private final CherryCore plugin;

    public SpawnCommands(CherryCore plugin) {
        this.plugin = plugin;
    }

    public CommandExecutor spawn() {
        return (sender, cmd, label, args) -> {
            if (!(sender instanceof Player p)) {
                MessageUtils.send(sender, "&cSolo i giocatori.");
                return true;
            }
            Location loc = plugin.getDataManager().getSpawn();
            if (loc == null) {
                MessageUtils.sendPrefixed(p, "&cSpawn non impostato.");
                return true;
            }
            p.teleport(loc);
            MessageUtils.sendPrefixed(p, "&fTeleportato allo spawn.");
            return true;
        };
    }

    public CommandExecutor setspawn() {
        return (sender, cmd, label, args) -> {
            if (!(sender instanceof Player p)) {
                MessageUtils.send(sender, "&cSolo i giocatori.");
                return true;
            }
            plugin.getDataManager().setSpawn(p.getLocation());
            MessageUtils.sendPrefixed(p, "&fSpawn impostato.");
            return true;
        };
    }
}