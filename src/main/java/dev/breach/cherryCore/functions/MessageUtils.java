package dev.breach.cherryCore.functions;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MessageUtils {

    public static final String PREFIX        = color("&7ꜱᴇʀᴠᴇʀ &8»");
    public static final String AB_PREFIX     = color("&c⚠ &8·");
    public static final String AB_MESSAGE    = color("&cNon sei in Rec Mode!");
    public static final String ANN_PREFIX    = color("&6&l");
    public static final String ANN_SUBTITLE  = color("&7Preparati!");
    public static final int    ANN_DURATION  = 60;   // 3 secondi in ticks
    public static final int    ANN_FADE_IN   = 20;
    public static final int    ANN_FADE_OUT  = 20;

    public static String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
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
}