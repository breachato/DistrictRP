package dev.breach.DistrictRP.commands.ChatGate.managers;

import dev.breach.DistrictRP.DistrictRP;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class MessageManager {

    public static String getMessage(String key) {
        String msg = DistrictRP.get().getConfig().getString("chatgate.messages." + key);
        if (msg == null) return ColorManager.color("&cᴍᴇꜱꜱᴀɢɢɪᴏ ᴍᴀɴᴄᴀɴᴛᴇ: &f" + key);
        msg = msg.replace("%prefix%", getPrefix());
        return ColorManager.color(msg);
    }

    public static String getMessage(String key, Map<String, String> placeholders) {
        String msg = DistrictRP.get().getConfig().getString("chatgate.messages." + key);
        if (msg == null) return ColorManager.color("&cᴍᴇꜱꜱᴀɢɢɪᴏ ᴍᴀɴᴄᴀɴᴛᴇ: &f" + key);
        msg = msg.replace("%prefix%", getPrefix());
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            msg = msg.replace(entry.getKey(), entry.getValue());
        }
        return ColorManager.color(msg);
    }

    public static String getMessage(String key, CommandSender sender, Map<String, String> placeholders) {
        String msg = DistrictRP.get().getConfig().getString("chatgate.messages." + key);
        if (msg == null) return ColorManager.color("&cᴍᴇꜱꜱᴀɢɢɪᴏ ᴍᴀɴᴄᴀɴᴛᴇ: &f" + key);
        msg = msg.replace("%prefix%", getPrefix());
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            msg = msg.replace(entry.getKey(), entry.getValue());
        }
        if (sender instanceof Player && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            msg = PlaceholderAPI.setPlaceholders((Player) sender, msg);
        }
        return ColorManager.color(msg);
    }

    public static String getPrefix() {
        String prefix = DistrictRP.get().getConfig().getString("chatgate.messages.prefix");
        return prefix != null ? ColorManager.color(prefix) : "";
    }
}