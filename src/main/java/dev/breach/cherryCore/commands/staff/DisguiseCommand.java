package dev.breach.cherryCore.commands.staff;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Travestimento via SkinsRestorer + nick custom.
 * NON usa LibsDisguises (troppo pesante). Usa SkinsRestorer + setDisplayName.
 */
public class DisguiseCommand implements CommandExecutor {

    private final CherryCore plugin;

    public DisguiseCommand(CherryCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player p)) {
            MessageUtils.send(sender, "&cSolo i giocatori.");
            return true;
        }
        if (!p.hasPermission("cherrycore.disguise")) {
            MessageUtils.send(p, "&cNon hai il permesso.");
            return true;
        }
        if (args.length == 0) {
            MessageUtils.sendPrefixed(p, "&cUsa: /disguise <nick|off>");
            return true;
        }

        String arg = args[0];

        // === OFF ===
        if (arg.equalsIgnoreCase("off") || arg.equalsIgnoreCase("remove")) {
            String previous = plugin.getDataManager().getDisguise(p.getUniqueId());
            if (previous == null) {
                MessageUtils.sendPrefixed(p, "&cNon sei travestito.");
                return true;
            }
            plugin.getDataManager().setDisguise(p.getUniqueId(), null);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "skin clear " + p.getName());
            p.setDisplayName(p.getName());
            p.setPlayerListName(p.getName());
            MessageUtils.sendPrefixed(p, "&cTravestimento rimosso.");
            return true;
        }

        // === SET ===
        plugin.getDataManager().setDisguise(p.getUniqueId(), arg);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "skin set " + p.getName() + " " + arg);
        // Nick (limite 16 caratteri Minecraft, ma displayName puo essere piu lungo)
        String coloredNick = arg.length() > 16 ? arg.substring(0, 16) : arg;
        p.setDisplayName(coloredNick);
        p.setPlayerListName(coloredNick);
        MessageUtils.sendPrefixed(p, "&aOra sei travestito da &f" + arg + "&a.");
        return true;
    }
}