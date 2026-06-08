package dev.breach.cherryCore.commands.staff;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;

public class GamemodeCommands {

    private final CherryCore plugin;

    public GamemodeCommands(CherryCore plugin) {
        this.plugin = plugin;
    }

    public CommandExecutor gms()  { return create(GameMode.SURVIVAL,  "Survival");  }
    public CommandExecutor gmc()  { return create(GameMode.CREATIVE,  "Creative");  }
    public CommandExecutor gma()  { return create(GameMode.ADVENTURE, "Adventure"); }
    public CommandExecutor gmsp() { return create(GameMode.SPECTATOR, "Spectator"); }

    private CommandExecutor create(GameMode mode, String label) {
        return (sender, cmd, lbl, args) -> {
            if (!sender.hasPermission("cherrycore.gamemode")) {
                MessageUtils.send(sender, "&cNon hai il permesso.");
                return true;
            }
            Player target;
            if (args.length > 0) {
                target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    MessageUtils.send(sender, "&cGiocatore non trovato.");
                    return true;
                }
            } else {
                if (!(sender instanceof Player p)) {
                    MessageUtils.send(sender, "&cUsa: /" + lbl + " <player>");
                    return true;
                }
                target = p;
            }
            target.setGameMode(mode);
            MessageUtils.sendPrefixed(target, "&fGamemode: &d" + label + "&f.");
            if (!target.equals(sender)) {
                MessageUtils.sendPrefixed(sender, "&fGamemode di &d" + target.getName() + " &f→ &d" + label + "&f.");
            }
            return true;
        };
    }
}