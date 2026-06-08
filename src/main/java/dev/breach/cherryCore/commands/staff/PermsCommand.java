package dev.breach.cherryCore.commands.staff;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PermsCommand implements CommandExecutor {

    private final CherryCore plugin;

    public PermsCommand(CherryCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("perms.admin")) {
            MessageUtils.send(sender, "&c✗ Non hai accesso.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "list" -> {
                MessageUtils.send(sender, "&7Whitelist attuale:");
                List<String> list = plugin.getWhitelist().all();
                if (list.isEmpty()) {
                    MessageUtils.send(sender, "&7Nessun giocatore autorizzato.");
                    return true;
                }
                for (String uuid : list) {
                    String name = plugin.getWhitelist().name(uuid);
                    MessageUtils.send(sender, "&7- &a" + name);
                }
            }
            case "add" -> {
                if (args.length < 2) {
                    MessageUtils.send(sender, "&7Usa: &b/perms add <player>");
                    return true;
                }
                OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
                if (plugin.getWhitelist().add(op)) {
                    MessageUtils.send(sender, "&a" + args[1] + " &7aggiunto alla whitelist.");
                } else {
                    MessageUtils.send(sender, "&b" + args[1] + " &7è già in whitelist.");
                }
            }
            case "remove" -> {
                if (args.length < 2) {
                    MessageUtils.send(sender, "&7Usa: &b/perms remove <player>");
                    return true;
                }
                OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
                if (plugin.getWhitelist().remove(op)) {
                    MessageUtils.send(sender, "&c" + args[1] + " &7rimosso dalla whitelist.");
                } else {
                    MessageUtils.send(sender, "&7" + args[1] + " &7non è in whitelist.");
                }
            }
            default -> sendUsage(sender);
        }
        return true;
    }

    private void sendUsage(CommandSender s) {
        MessageUtils.send(s, "&7Usa: &b/perms list&7, &a/perms add <player>&7, &c/perms remove <player>");
    }
}