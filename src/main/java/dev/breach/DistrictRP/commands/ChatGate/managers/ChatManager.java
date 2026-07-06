package dev.breach.DistrictRP.commands.ChatGate.managers;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.ChatGate.ChatGate;
import dev.breach.DistrictRP.commands.ChatGate.models.CustomChat;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatManager {

    private static final ChatManager instance = new ChatManager();

    public static ChatManager getInstance() {
        return instance;
    }

    private final Map<UUID, String> toggledChats = new HashMap<>();
    private final Map<UUID, Long> lastMessageTime = new HashMap<>();
    private final Map<UUID, String> lastMessageContent = new HashMap<>();
    private final Map<UUID, Integer> duplicateCount = new HashMap<>();

    public boolean toggleChat(Player player, CustomChat chat) {
        UUID uuid = player.getUniqueId();
        String current = toggledChats.get(uuid);

        if (chat.id().equalsIgnoreCase(current)) {
            toggledChats.remove(uuid);
            return false;
        } else {
            toggledChats.put(uuid, chat.id());
            return true;
        }
    }

    public boolean hasToggledChat(Player player) {
        return toggledChats.containsKey(player.getUniqueId());
    }

    public CustomChat getToggledChat(Player player) {
        String chatId = toggledChats.get(player.getUniqueId());
        if (chatId == null) return null;
        return ChatGate.getInstance().getChats().get(chatId);
    }

    private long getCooldownMs() {
        return DistrictRP.get().getConfig().getLong("chatgate.settings.antispam.cooldown-ms", 2000L);
    }

    private int getDuplicateBlock() {
        return DistrictRP.get().getConfig().getInt("chatgate.settings.antispam.duplicate-block", 3);
    }

    public String checkAntiSpam(Player player, String message) {
        if (player.hasPermission("chatgate.antispam.bypass")) return null;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = lastMessageTime.getOrDefault(uuid, 0L);
        long cooldown = getCooldownMs();

        if (now - last < cooldown) {
            long remaining = (cooldown - (now - last)) / 1000 + 1;
            Map<String, String> ph = new HashMap<>();
            ph.put("%seconds%", String.valueOf(remaining));
            return MessageManager.getMessage("antispam-cooldown", ph);
        }

        String lastContent = lastMessageContent.getOrDefault(uuid, "");
        if (message.equalsIgnoreCase(lastContent)) {
            int count = duplicateCount.getOrDefault(uuid, 0) + 1;
            duplicateCount.put(uuid, count);
            if (count >= getDuplicateBlock()) {
                return MessageManager.getMessage("antispam-duplicate");
            }
        } else {
            duplicateCount.put(uuid, 0);
        }

        return null;
    }

    private void updateAntiSpam(Player player, String message) {
        UUID uuid = player.getUniqueId();
        lastMessageTime.put(uuid, System.currentTimeMillis());
        lastMessageContent.put(uuid, message);
    }

    public void sendChatMessage(CustomChat chat, Player sender, String message) {
        String spamError = checkAntiSpam(sender, message);
        if (spamError != null) {
            sender.sendMessage(spamError);
            return;
        }

        updateAntiSpam(sender, message);

        String format = chat.format();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            format = PlaceholderAPI.setPlaceholders(sender, format);
        }

        format = format.replace("%player%", sender.getName())
                .replace("%message%", message);

        format = ColorManager.color(format);

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.hasPermission("chatgate.chats." + chat.id())) {
                target.sendMessage(format);
            }
        }

        if (DistrictRP.get().getConfig().getBoolean("chatgate.settings.log-chat", true)) {
            String cleanLog = "[" + chat.displayName().replaceAll("§.", "") + "] "
                    + sender.getName() + ": " + message;
            Bukkit.getLogger().info(cleanLog);
        }
    }

    public void removeToggled(Player player) {
        UUID uuid = player.getUniqueId();
        toggledChats.remove(uuid);
        lastMessageTime.remove(uuid);
        lastMessageContent.remove(uuid);
        duplicateCount.remove(uuid);
    }
}