package dev.breach.DistrictRP.commands.ChatGate.commands;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.ChatGate.ChatGate;
import dev.breach.DistrictRP.commands.ChatGate.managers.ColorManager;
import dev.breach.DistrictRP.commands.ChatGate.managers.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatGateCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(MessageManager.getMessage("unknown-subcommand"));
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload" -> {
                if (!sender.hasPermission("chatgate.reload")) {
                    sender.sendMessage(MessageManager.getMessage("no-permission"));
                    return true;
                }
                DistrictRP.get().reloadConfig();
                ChatGate.getInstance().loadChats();
                sender.sendMessage(MessageManager.getMessage("config-reloaded"));
                return true;
            }
            case "create" -> {
                if (!sender.hasPermission("chatgate.create")) {
                    sender.sendMessage(MessageManager.getMessage("no-permission"));
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(MessageManager.getMessage("create-usage"));
                    return true;
                }
                String id = args[1].toLowerCase();
                if (ChatGate.getInstance().getChats().containsKey(id)) {
                    sender.sendMessage(MessageManager.getMessage("chat-already-exists"));
                    return true;
                }
                String displayName = args[2];
                StringBuilder formatBuilder = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    formatBuilder.append(args[i]).append(" ");
                }
                String format = formatBuilder.toString().trim();

                FileConfiguration config = DistrictRP.get().getConfig();
                String path = "chatgate.chats." + id;

                config.set(path + ".display-name", displayName);
                config.set(path + ".format", format);
                DistrictRP.get().saveConfig();
                ChatGate.getInstance().loadChats();

                sender.sendMessage(MessageManager.getMessage("chat-created"));
                return true;
            }
            case "delete" -> {
                if (!sender.hasPermission("chatgate.delete")) {
                    sender.sendMessage(MessageManager.getMessage("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(MessageManager.getMessage("delete-usage"));
                    return true;
                }
                String id = args[1].toLowerCase();
                FileConfiguration config = DistrictRP.get().getConfig();

                if (!config.contains("chatgate.chats." + id)) {
                    sender.sendMessage(MessageManager.getMessage("chat-not-exists"));
                    return true;
                }
                config.set("chatgate.chats." + id, null);
                DistrictRP.get().saveConfig();
                ChatGate.getInstance().loadChats();

                sender.sendMessage(MessageManager.getMessage("chat-deleted"));
                return true;
            }
            case "help" -> {
                sendHelp(sender);
                return true;
            }
            default -> {
                sender.sendMessage(MessageManager.getMessage("unknown-subcommand"));
                return true;
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        FileConfiguration config = DistrictRP.get().getConfig();
        List<String> helpLines = config.getStringList("chatgate.messages.help");
        if (helpLines.isEmpty()) {
            sender.sendMessage(MessageManager.getMessage("unknown-subcommand"));
            return;
        }
        for (String line : helpLines) {
            sender.sendMessage(ColorManager.color(line));
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "delete", "reload", "help"));
            return completions;
        }
        if (args[0].equalsIgnoreCase("delete") && args.length == 2) {
            completions.addAll(ChatGate.getInstance().getChats().keySet());
        }
        return completions;
    }
}