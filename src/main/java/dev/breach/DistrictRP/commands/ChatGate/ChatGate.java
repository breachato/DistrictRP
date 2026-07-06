package dev.breach.DistrictRP.commands.ChatGate;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.ChatGate.commands.ChatCommand;
import dev.breach.DistrictRP.commands.ChatGate.commands.ChatGateCommand;
import dev.breach.DistrictRP.commands.ChatGate.listeners.ChatListener;
import dev.breach.DistrictRP.commands.ChatGate.models.CustomChat;
import dev.breach.DistrictRP.commands.ChatGate.placeholders.PlaceholderAPIExpansion;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public final class ChatGate {

    private static ChatGate instance;
    private final DistrictRP plugin;
    private final Map<String, CustomChat> chats = new HashMap<>();

    public ChatGate(DistrictRP plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public void enable() {
        long start = System.currentTimeMillis();

        loadChats();

        Bukkit.getPluginManager().registerEvents(new ChatListener(), plugin);

        if (plugin.getCommand("chat") != null) {
            ChatCommand chatCmd = new ChatCommand();
            plugin.getCommand("chat").setExecutor(chatCmd);
            plugin.getCommand("chat").setTabCompleter(chatCmd);
        }
        if (plugin.getCommand("chatgate") != null) {
            ChatGateCommand cgCmd = new ChatGateCommand();
            plugin.getCommand("chatgate").setExecutor(cgCmd);
            plugin.getCommand("chatgate").setTabCompleter(cgCmd);
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPIExpansion().register();
            plugin.getLogger().info("[ChatGate] PlaceholderAPI hook registrato");
        }

        long elapsed = System.currentTimeMillis() - start;
        plugin.getLogger().info("[ChatGate] Modulo abilitato in " + elapsed + "ms (" + chats.size() + " chat caricate)");
    }

    public void disable() {
        chats.clear();
    }

    public void loadChats() {
        chats.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("chatgate.chats");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String format = section.getString(key + ".format");
                String displayName = section.getString(key + ".display-name");
                chats.put(key.toLowerCase(), new CustomChat(key, format, displayName));
            }
        }
    }

    public Map<String, CustomChat> getChats() {
        return chats;
    }

    public DistrictRP getPlugin() {
        return plugin;
    }

    public static ChatGate getInstance() {
        return instance;
    }
}