package dev.breach.DistrictRP.commands.ChatGate;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.ChatGate.placeholders.PlaceholderAPIExpansion;
import dev.breach.DistrictRP.functions.MessageUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sistema canali chat custom (toggle per-player, antispam, formati da config).
 * Raccoglie stato + listener + messaggi in un'unica classe; i comandi /chat e
 * /chatgate stanno in {@link dev.breach.DistrictRP.commands.ChatGate.commands.ChatGateCommands}.
 */
public final class ChatGate implements Listener {

    public record CustomChat(String id, String format, String displayName) {}

    private static ChatGate instance;
    private final DistrictRP plugin;
    private final Map<String, CustomChat> chats = new HashMap<>();

    private final Map<UUID, String> toggledChats = new HashMap<>();
    private final Map<UUID, Long> lastMessageTime = new HashMap<>();
    private final Map<UUID, String> lastMessageContent = new HashMap<>();
    private final Map<UUID, Integer> duplicateCount = new HashMap<>();

    public ChatGate(DistrictRP plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public void enable() {
        loadChats();
        Bukkit.getPluginManager().registerEvents(this, plugin);

        var commands = new dev.breach.DistrictRP.commands.ChatGate.commands.ChatGateCommands();
        bind("chat", commands);
        bind("chatgate", commands);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPIExpansion().register();
            plugin.getLogger().info("[ChatGate] PlaceholderAPI hook registrato");
        }
        plugin.getLogger().info("[ChatGate] Modulo abilitato (" + chats.size() + " chat caricate)");
    }

    private void bind(String name, dev.breach.DistrictRP.commands.ChatGate.commands.ChatGateCommands exec) {
        if (plugin.getCommand(name) == null) return;
        plugin.getCommand(name).setExecutor(exec);
        plugin.getCommand(name).setTabCompleter(exec);
    }

    public void disable() {
        chats.clear();
    }

    public void loadChats() {
        chats.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("chatgate.chats");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                chats.put(key.toLowerCase(), new CustomChat(
                        key, section.getString(key + ".format"), section.getString(key + ".display-name")));
            }
        }
    }

    public Map<String, CustomChat> getChats() { return chats; }
    public DistrictRP getPlugin() { return plugin; }
    public static ChatGate getInstance() { return instance; }

    // --- toggle / stato per-player ---

    public boolean toggleChat(Player player, CustomChat chat) {
        UUID uuid = player.getUniqueId();
        if (chat.id().equalsIgnoreCase(toggledChats.get(uuid))) {
            toggledChats.remove(uuid);
            return false;
        }
        toggledChats.put(uuid, chat.id());
        return true;
    }

    public boolean hasToggledChat(Player player) {
        return toggledChats.containsKey(player.getUniqueId());
    }

    public CustomChat getToggledChat(Player player) {
        String chatId = toggledChats.get(player.getUniqueId());
        return chatId == null ? null : chats.get(chatId);
    }

    public void removeToggled(Player player) {
        UUID uuid = player.getUniqueId();
        toggledChats.remove(uuid);
        lastMessageTime.remove(uuid);
        lastMessageContent.remove(uuid);
        duplicateCount.remove(uuid);
    }

    // --- antispam + invio ---

    public void sendChatMessage(CustomChat chat, Player sender, String message) {
        String spamError = checkAntiSpam(sender, message);
        if (spamError != null) {
            sender.sendMessage(spamError);
            return;
        }
        lastMessageTime.put(sender.getUniqueId(), System.currentTimeMillis());
        lastMessageContent.put(sender.getUniqueId(), message);

        String format = chat.format();
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            format = PlaceholderAPI.setPlaceholders(sender, format);
        }
        format = MessageUtils.color(format
                .replace("%player%", sender.getName())
                .replace("%message%", message));

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.hasPermission("chatgate.chats." + chat.id())) target.sendMessage(format);
        }

        if (plugin.getConfig().getBoolean("chatgate.settings.log-chat", true)) {
            Bukkit.getLogger().info("[" + chat.displayName().replaceAll("§.", "") + "] "
                    + sender.getName() + ": " + message);
        }
    }

    private String checkAntiSpam(Player player, String message) {
        if (player.hasPermission("chatgate.antispam.bypass")) return null;
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long cooldown = plugin.getConfig().getLong("chatgate.settings.antispam.cooldown-ms", 2000L);
        long last = lastMessageTime.getOrDefault(uuid, 0L);
        if (now - last < cooldown) {
            long remaining = (cooldown - (now - last)) / 1000 + 1;
            Map<String, String> ph = new HashMap<>();
            ph.put("%seconds%", String.valueOf(remaining));
            return message("antispam-cooldown", ph);
        }
        String lastContent = lastMessageContent.getOrDefault(uuid, "");
        if (message.equalsIgnoreCase(lastContent)) {
            int count = duplicateCount.getOrDefault(uuid, 0) + 1;
            duplicateCount.put(uuid, count);
            if (count >= plugin.getConfig().getInt("chatgate.settings.antispam.duplicate-block", 3)) {
                return message("antispam-duplicate");
            }
        } else {
            duplicateCount.put(uuid, 0);
        }
        return null;
    }

    // --- messaggi da config (chatgate.messages.*) ---

    public String prefix() {
        String p = plugin.getConfig().getString("chatgate.messages.prefix");
        return p != null ? MessageUtils.color(p) : "";
    }

    public String message(String key) {
        String msg = plugin.getConfig().getString("chatgate.messages." + key);
        if (msg == null) return MessageUtils.color("&cᴍᴇꜱꜱᴀɢɢɪᴏ ᴍᴀɴᴄᴀɴᴛᴇ: &f" + key);
        return MessageUtils.color(msg.replace("%prefix%", prefix()));
    }

    public String message(String key, Map<String, String> placeholders) {
        String msg = plugin.getConfig().getString("chatgate.messages." + key);
        if (msg == null) return MessageUtils.color("&cᴍᴇꜱꜱᴀɢɢɪᴏ ᴍᴀɴᴄᴀɴᴛᴇ: &f" + key);
        msg = msg.replace("%prefix%", prefix());
        for (Map.Entry<String, String> e : placeholders.entrySet()) msg = msg.replace(e.getKey(), e.getValue());
        return MessageUtils.color(msg);
    }

    // --- listener ---

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!hasToggledChat(player)) return;
        CustomChat chat = getToggledChat(player);
        if (chat == null) return;
        event.setCancelled(true);
        sendChatMessage(chat, player, event.getMessage());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeToggled(event.getPlayer());
    }
}
