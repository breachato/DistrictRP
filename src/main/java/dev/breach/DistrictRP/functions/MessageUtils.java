package dev.breach.DistrictRP.functions;

import dev.breach.DistrictRP.DistrictRP;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {

    public static final String PREFIX;

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    static {
        PREFIX = ChatColor.translateAlternateColorCodes('&', "&7ꜱᴇʀᴠᴇʀ &8»");
    }

    private static FileConfiguration messages;
    private static File messagesFile;
    private static String msgPrefix = "";

    public static String color(String s) {
        if (s == null || s.isEmpty()) return "";
        try {
            Matcher m = HEX_PATTERN.matcher(s);
            StringBuffer buf = new StringBuffer();
            while (m.find()) {
                String hex = m.group(1);
                m.appendReplacement(buf, net.md_5.bungee.api.ChatColor.of("#" + hex).toString());
            }
            m.appendTail(buf);
            return ChatColor.translateAlternateColorCodes('&', buf.toString());
        } catch (Throwable t) {
            return ChatColor.translateAlternateColorCodes('&', s);
        }
    }

    public static void send(CommandSender s, String msg) {
        s.sendMessage(color(msg));
    }

    public static void sendPrefixed(CommandSender s, String msg) {
        s.sendMessage(color(PREFIX + " " + msg));
    }

    public static void broadcast(String msg) {
        Bukkit.broadcastMessage(color(msg));
    }

    public static void actionBar(Player p, String msg) {
        p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(color(msg)));
    }

    public static void title(Player p, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        p.sendTitle(color(title), color(subtitle), fadeIn, stay, fadeOut);
    }

    public static void load(DistrictRP plugin) {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            try {
                plugin.saveResource("messages.yml", false);
            } catch (Throwable ignored) {
                try { messagesFile.createNewFile(); } catch (IOException ignored2) {}
            }
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        try {
            if (plugin.getResource("messages.yml") != null) {
                InputStreamReader in = new InputStreamReader(
                        plugin.getResource("messages.yml"), StandardCharsets.UTF_8);
                YamlConfiguration def = YamlConfiguration.loadConfiguration(in);
                messages.setDefaults(def);
                messages.options().copyDefaults(true);
                messages.save(messagesFile);
                in.close();
            }
        } catch (IOException ignored) {}
        msgPrefix = color(messages.getString("prefix", ""));
    }

    public static void reload() {
        if (messagesFile == null) return;
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        msgPrefix = color(messages.getString("prefix", ""));
    }

    public static String getPrefix() {
        return msgPrefix;
    }

    public static String get(String path) {
        if (messages == null) return color("&cMessages not loaded: " + path);
        String raw = messages.getString(path, "&cMissing: " + path);
        return color(raw.replace("%prefix%", msgPrefix));
    }

    public static String get(String path, String... replacements) {
        String s = get(path);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            s = s.replace("%" + replacements[i] + "%", replacements[i + 1]);
        }
        return s;
    }

    public static List<String> getList(String path) {
        List<String> out = new ArrayList<>();
        if (messages == null) return out;
        for (String line : messages.getStringList(path)) {
            out.add(color(line.replace("%prefix%", msgPrefix)));
        }
        return out;
    }

    public static List<String> getList(String path, String... replacements) {
        List<String> raw = getList(path);
        List<String> out = new ArrayList<>();
        for (String line : raw) {
            String s = line;
            for (int i = 0; i + 1 < replacements.length; i += 2) {
                s = s.replace("%" + replacements[i] + "%", replacements[i + 1]);
            }
            out.add(s);
        }
        return out;
    }

    public static void sendMsg(CommandSender sender, String path) {
        sender.sendMessage(get(path));
    }

    public static void sendMsg(CommandSender sender, String path, String... replacements) {
        sender.sendMessage(get(path, replacements));
    }

    public static void sendList(CommandSender sender, String path) {
        for (String line : getList(path)) sender.sendMessage(line);
    }

    public static void sendList(CommandSender sender, String path, String... replacements) {
        for (String line : getList(path, replacements)) sender.sendMessage(line);
    }
}