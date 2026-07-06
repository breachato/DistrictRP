package dev.breach.DistrictRP.commands.roleplay.chat;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatSymCommand implements CommandExecutor, TabCompleter {

    private final DistrictRP plugin;
    private final ChatSymManager manager;

    public ChatSymCommand(DistrictRP plugin, ChatSymManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String perm = plugin.getConfig().getString("chatsym.permission", "DistrictRP.staffchatsym");
        if (!sender.hasPermission(perm)) {
            MessageUtils.sendMsg(sender, "general.no-permission");
            return true;
        }

        if (args.length == 0) {
            MessageUtils.sendList(sender, "chatsym.help");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "aggiungi" -> {
                if (args.length < 3) {
                    MessageUtils.sendList(sender, "chatsym.help");
                    return true;
                }
                String name = args[1].startsWith("/") ? args[1].substring(1) : args[1];
                String symbol = args[2];
                if (!manager.add(symbol, name)) {
                    MessageUtils.sendMsg(sender, "chatsym.already-exists");
                    return true;
                }
                MessageUtils.sendMsg(sender, "chatsym.added", "symbol", symbol, "command", name);
            }
            case "rimuovi" -> {
                if (args.length < 2) {
                    MessageUtils.sendList(sender, "chatsym.help");
                    return true;
                }
                String symbol = args[1];
                if (!manager.remove(symbol)) {
                    MessageUtils.sendMsg(sender, "chatsym.not-found");
                    return true;
                }
                MessageUtils.sendMsg(sender, "chatsym.removed", "symbol", symbol);
            }
            default -> MessageUtils.sendList(sender, "chatsym.help");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("aggiungi", "rimuovi");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("rimuovi")) {
            return new ArrayList<>(manager.getSymbols().keySet());
        }
        return new ArrayList<>();
    }
}