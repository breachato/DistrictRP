package dev.breach.DistrictRP.commands.staff;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;

public class GamemodeCommands {

    private final DistrictRP plugin;

    public GamemodeCommands(DistrictRP plugin) {
        this.plugin = plugin;
    }

    public CommandExecutor gms()  { return create(GameMode.SURVIVAL,  "Survival");  }
    public CommandExecutor gmc()  { return create(GameMode.CREATIVE,  "Creative");  }
    public CommandExecutor gma()  { return create(GameMode.ADVENTURE, "Adventure"); }
    public CommandExecutor gmsp() { return create(GameMode.SPECTATOR, "Spectator"); }

    private CommandExecutor create(GameMode mode, String label) {
        return (sender, cmd, lbl, args) -> {
            if (!sender.hasPermission("DistrictRP.gamemode")) {
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
            MessageUtils.sendPrefixed(target, "&fGamemode: &e" + label + "&f.");
            if (!target.equals(sender)) {
                MessageUtils.sendPrefixed(sender, "&fGamemode di &e" + target.getName() + " &f→ &e" + label + "&f.");
            }
            return true;
        };
    }
}