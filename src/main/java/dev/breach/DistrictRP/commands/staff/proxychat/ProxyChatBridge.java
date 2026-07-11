package dev.breach.DistrictRP.commands.staff.proxychat;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ProxyChatBridge {

    public static final String CHANNEL = "districtrp:staffchat";

    public static void sendToProxy(DistrictRP plugin, Player sender, String channelId, String message) {
        boolean enabled = plugin.getConfig().getBoolean("proxy-chat.enabled", true);
        boolean sendToProxy = plugin.getConfig().getBoolean("proxy-chat.send-to-proxy", true);

        String serverId = plugin.getConfig().getString("server-id", Bukkit.getServer().getName());
        String rankSymbol = resolveRankSymbol(plugin, sender);

        localEcho(plugin, sender, channelId, serverId, rankSymbol, message);

        if (!enabled || !sendToProxy) return;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeUTF("CHAT");
            out.writeUTF(channelId);
            out.writeUTF(serverId);
            out.writeUTF(sender.getName());
            out.writeUTF(rankSymbol == null ? "" : rankSymbol);
            out.writeUTF(message);
            sender.sendPluginMessage(plugin, CHANNEL, baos.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning("[ProxyChat] Errore invio proxy: " + e.getMessage());
            MessageUtils.sendMsg(sender, "proxychat.proxy-unavailable");
        }
    }

    private static void localEcho(DistrictRP plugin, Player sender, String channelId,
                                  String serverId, String rankSymbol, String message) {
        ConfigurationSection ch = plugin.getConfig().getConfigurationSection(
                "proxy-chat.channels." + channelId);
        if (ch == null) return;
        String permission = ch.getString("permission", "");
        String format = ch.getString("local-echo-format", "");
        if (format == null || format.isEmpty()) return;

        String legacy = format
                .replace("%server%", serverId)
                .replace("%player%", sender.getName())
                .replace("%rank%", rankSymbol == null ? "" : rankSymbol)
                .replace("%message%", message);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            legacy = PlaceholderAPI.setPlaceholders(sender, legacy);
        }

        String colored = MessageUtils.color(legacy);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (permission.isEmpty() || online.hasPermission(permission)) {
                online.sendMessage(colored);
            }
        }
    }

    private static String resolveRankSymbol(DistrictRP plugin, Player p) {
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("stafflist.ranks");
        if (ranks == null) return "";
        for (String order : plugin.getConfig().getStringList("stafflist.order")) {
            ConfigurationSection r = ranks.getConfigurationSection(order);
            if (r == null) continue;
            String perm = r.getString("permission", "");
            String symbol = r.getString("symbol", "");
            if (!perm.isEmpty() && p.hasPermission(perm) && symbol != null && !symbol.isEmpty()) {
                return symbol + " ";
            }
        }
        return "";
    }
}