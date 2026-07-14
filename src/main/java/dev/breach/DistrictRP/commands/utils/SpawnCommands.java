package dev.breach.DistrictRP.commands.utils;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpawnCommands {

    private final DistrictRP plugin;

    public SpawnCommands(DistrictRP plugin) {
        this.plugin = plugin;
    }

    public CommandExecutor spawn() {
        return (sender, cmd, label, args) -> {
            String perm = plugin.getConfig().getString("spawn.permission", "DistrictRP.spawn");
            if (!sender.hasPermission(perm)) {
                MessageUtils.sendMsg(sender, "general.no-permission");
                return true;
            }
            if (!(sender instanceof Player p)) {
                MessageUtils.sendMsg(sender, "general.only-player");
                return true;
            }
            String world = args.length >= 1
                    ? args[0]
                    : p.getWorld().getName();

            if (!plugin.getDataManager().hasSpawn(world)) {
                MessageUtils.sendMsg(p, "spawn.no-spawn-set");
                return true;
            }
            Location loc = plugin.getDataManager().getSpawn(world);
            if (loc == null || loc.getWorld() == null) {
                MessageUtils.sendMsg(p, "spawn.world-not-found", "world", world);
                return true;
            }
            plugin.back.put(p.getUniqueId(), p.getLocation());
            p.teleport(loc);
            MessageUtils.sendMsg(p, "spawn.teleported", "world", world);
            return true;
        };
    }

    public CommandExecutor back() {
        return (sender, cmd, label, args) -> {
            if (!(sender instanceof Player p)) {
                MessageUtils.send(sender, "&cSolo i giocatori.");
                return true;
            }
            Location loc = plugin.back.get(p.getUniqueId());
            if (loc == null) {
                MessageUtils.sendPrefixed(p, "&cNessuna posizione salvata.");
                return true;
            }
            p.teleport(loc);
            MessageUtils.sendPrefixed(p, "&fTornato alla posizione precedente.");
            return true;
        };
    }

    public CommandExecutor setSpawn() {
        return (sender, cmd, label, args) -> {
            String perm = plugin.getConfig().getString("spawn.set-permission", "DistrictRP.setspawn");
            if (!sender.hasPermission(perm)) {
                MessageUtils.sendMsg(sender, "general.no-permission");
                return true;
            }
            if (!(sender instanceof Player p)) {
                MessageUtils.sendMsg(sender, "general.only-player");
                return true;
            }
            String world = args.length >= 1 ? args[0] : p.getWorld().getName();
            plugin.getDataManager().setSpawn(world, p.getLocation());
            MessageUtils.sendMsg(p, "spawn.set", "world", world);
            return true;
        };
    }
}