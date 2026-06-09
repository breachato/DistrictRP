package dev.breach.cherryCore.commands.utils;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MsgCommand implements CommandExecutor {

    private final CherryCore plugin;

    public MsgCommand(CherryCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            MessageUtils.send(sender, "&cSolo i giocatori.");
            return true;
        }
        if (args.length < 2) {
            MessageUtils.send(p, "&cUsa: /msg <player> <messaggio>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            MessageUtils.send(p, "&cGiocatore non trovato.");
            return true;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) sb.append(" ");
            sb.append(args[i]);
        }
        String msg = sb.toString();

        MessageUtils.send(p,      "&7Tu &8» &7" + target.getName() + ": " + msg);
        MessageUtils.send(target, "&7" + p.getName() + " &8» &7Te: " + msg);

        plugin.reply.put(p.getUniqueId(),      target.getUniqueId());
        plugin.reply.put(target.getUniqueId(), p.getUniqueId());
        return true;
    }

    public CommandExecutor reply() {
        return (sender, cmd, label, args) -> {
            if (!(sender instanceof Player p)) {
                MessageUtils.send(sender, "&cSolo i giocatori.");
                return true;
            }
            if (args.length == 0) {
                MessageUtils.send(p, "&cUsa: /reply <messaggio>");
                return true;
            }
            java.util.UUID targetUuid = plugin.reply.get(p.getUniqueId());
            if (targetUuid == null) {
                MessageUtils.sendPrefixed(p, "&cNessuno a cui rispondere.");
                return true;
            }
            Player target = Bukkit.getPlayer(targetUuid);
            if (target == null) {
                MessageUtils.sendPrefixed(p, "&cIl giocatore non è online.");
                return true;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(" ");
                sb.append(args[i]);
            }
            String msg = sb.toString();
            MessageUtils.send(p,      "&f[&fTu &f-> &f" + target.getName() + "&f] " + msg);
            MessageUtils.send(target, "&f[&f" + p.getName() + " &f-> &fTe&f] " + msg);
            plugin.reply.put(target.getUniqueId(), p.getUniqueId());
            return true;
        };
    }
}