package dev.breach.DistrictRP.commands.utils;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BackCommand implements CommandExecutor {

    private final DistrictRP plugin;

    public BackCommand(DistrictRP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
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
    }
}