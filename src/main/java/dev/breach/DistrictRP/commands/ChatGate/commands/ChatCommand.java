package dev.breach.DistrictRP.commands.ChatGate.commands;

import dev.breach.DistrictRP.commands.ChatGate.ChatGate;
import dev.breach.DistrictRP.commands.ChatGate.managers.ChatManager;
import dev.breach.DistrictRP.commands.ChatGate.managers.MessageManager;
import dev.breach.DistrictRP.commands.ChatGate.models.CustomChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSolo i giocatori possono usare questo comando.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(MessageManager.getMessage("chat-usage"));
            return true;
        }

        String chatId = args[0].toLowerCase();
        CustomChat chat = ChatGate.getInstance().getChats().get(chatId);

        if (chat == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%chat%", args[0]);
            player.sendMessage(MessageManager.getMessage("chat-not-found", placeholders));
            return true;
        }

        String permission = "chatgate.chats." + chatId;
        if (!player.hasPermission(permission)) {
            player.sendMessage(MessageManager.getMessage("no-permission"));
            return true;
        }

        if (args.length > 1) {
            String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            ChatManager.getInstance().sendChatMessage(chat, player, message);
            return true;
        }

        boolean toggled = ChatManager.getInstance().toggleChat(player, chat);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%chat%", chat.displayName());

        if (toggled) {
            player.sendMessage(MessageManager.getMessage("chat-enabled", placeholders));
        } else {
            player.sendMessage(MessageManager.getMessage("chat-disabled", placeholders));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            for (String chatId : ChatGate.getInstance().getChats().keySet()) {
                if (player.hasPermission("chatgate.chats." + chatId)) {
                    suggestions.add(chatId);
                }
            }
            return suggestions;
        }

        return Collections.emptyList();
    }
}