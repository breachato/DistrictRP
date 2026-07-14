package dev.breach.DistrictRP.commands.ChatGate.commands;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.ChatGate.ChatGate;
import dev.breach.DistrictRP.commands.ChatGate.ChatGate.CustomChat;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Comandi ChatGate: /chat (player: toggle/invio) e /chatgate (admin:
 * create/delete/reload). Un solo executor che smista su command.getName().
 */
public class ChatGateCommands implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        return command.getName().equalsIgnoreCase("chatgate")
                ? gate(sender, args)
                : chat(sender, args);
    }

    private boolean chat(CommandSender sender, String[] args) {
        ChatGate gate = ChatGate.getInstance();
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSolo i giocatori possono usare questo comando.");
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(gate.message("chat-usage"));
            return true;
        }
        String chatId = args[0].toLowerCase();
        CustomChat chat = gate.getChats().get(chatId);
        if (chat == null) {
            Map<String, String> ph = new HashMap<>();
            ph.put("%chat%", args[0]);
            player.sendMessage(gate.message("chat-not-found", ph));
            return true;
        }
        if (!player.hasPermission("chatgate.chats." + chatId)) {
            player.sendMessage(gate.message("no-permission"));
            return true;
        }
        if (args.length > 1) {
            gate.sendChatMessage(chat, player, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
            return true;
        }
        boolean toggled = gate.toggleChat(player, chat);
        Map<String, String> ph = new HashMap<>();
        ph.put("%chat%", chat.displayName());
        player.sendMessage(gate.message(toggled ? "chat-enabled" : "chat-disabled", ph));
        return true;
    }

    private boolean gate(CommandSender sender, String[] args) {
        ChatGate gate = ChatGate.getInstance();
        if (args.length == 0) {
            sender.sendMessage(gate.message("unknown-subcommand"));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("chatgate.reload")) {
                    sender.sendMessage(gate.message("no-permission"));
                    return true;
                }
                DistrictRP.get().reloadConfig();
                gate.loadChats();
                sender.sendMessage(gate.message("config-reloaded"));
            }
            case "create" -> {
                if (!sender.hasPermission("chatgate.create")) {
                    sender.sendMessage(gate.message("no-permission"));
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(gate.message("create-usage"));
                    return true;
                }
                String id = args[1].toLowerCase();
                if (gate.getChats().containsKey(id)) {
                    sender.sendMessage(gate.message("chat-already-exists"));
                    return true;
                }
                String format = String.join(" ", Arrays.copyOfRange(args, 3, args.length)).trim();
                FileConfiguration config = DistrictRP.get().getConfig();
                config.set("chatgate.chats." + id + ".display-name", args[2]);
                config.set("chatgate.chats." + id + ".format", format);
                DistrictRP.get().saveConfig();
                gate.loadChats();
                sender.sendMessage(gate.message("chat-created"));
            }
            case "delete" -> {
                if (!sender.hasPermission("chatgate.delete")) {
                    sender.sendMessage(gate.message("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(gate.message("delete-usage"));
                    return true;
                }
                String id = args[1].toLowerCase();
                FileConfiguration config = DistrictRP.get().getConfig();
                if (!config.contains("chatgate.chats." + id)) {
                    sender.sendMessage(gate.message("chat-not-exists"));
                    return true;
                }
                config.set("chatgate.chats." + id, null);
                DistrictRP.get().saveConfig();
                gate.loadChats();
                sender.sendMessage(gate.message("chat-deleted"));
            }
            case "help" -> sendHelp(sender);
            default -> sender.sendMessage(gate.message("unknown-subcommand"));
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        List<String> helpLines = DistrictRP.get().getConfig().getStringList("chatgate.messages.help");
        if (helpLines.isEmpty()) {
            sender.sendMessage(ChatGate.getInstance().message("unknown-subcommand"));
            return;
        }
        for (String line : helpLines) sender.sendMessage(MessageUtils.color(line));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("chatgate")) {
            if (args.length == 1) return new ArrayList<>(Arrays.asList("create", "delete", "reload", "help"));
            if (args[0].equalsIgnoreCase("delete") && args.length == 2) {
                return new ArrayList<>(ChatGate.getInstance().getChats().keySet());
            }
            return Collections.emptyList();
        }
        if (sender instanceof Player player && args.length == 1) {
            List<String> out = new ArrayList<>();
            for (String chatId : ChatGate.getInstance().getChats().keySet()) {
                if (player.hasPermission("chatgate.chats." + chatId)) out.add(chatId);
            }
            return out;
        }
        return Collections.emptyList();
    }
}
